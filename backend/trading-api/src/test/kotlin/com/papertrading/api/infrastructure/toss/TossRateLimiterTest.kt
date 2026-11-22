package com.papertrading.api.infrastructure.toss

import com.papertrading.api.common.exception.TossIntegrationDisabledException
import com.papertrading.api.common.exception.TossRateLimitedException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

// 그룹별 최소 간격(초당 한도의 역수)을 가짜 시계와 sleeper 로 확인, 한도는 설정값 (RULE-08, D16)
class TossRateLimiterTest {

    // 시험이 정한 임의 한도, 토스 정책 수치가 아님
    private fun properties(limits: Map<TossRateLimitGroup, Int>) = TossProperties().apply { rateLimits = limits }

    private class FakeTime {
        val clock = AtomicLong(1_000)
        val slept = mutableListOf<Long>()
        val sleeper: (Long) -> Unit = { millis ->
            synchronized(slept) { slept += millis }
            clock.addAndGet(millis)
        }

        fun limiterFor(properties: TossProperties) = TossRateLimiter(properties, clock::get, sleeper)
    }

    private fun limiter(limits: Map<TossRateLimitGroup, Int>, time: FakeTime) = time.limiterFor(properties(limits))

    @ParameterizedTest(name = "초당 {0}회 한도는 연속 호출 사이에 {1}ms 를 둔다")
    @CsvSource("1, 1000", "5, 200", "6, 167", "15, 67", "20, 50")
    fun `초당 한도의 역수가 최소 간격이고 나누어떨어지지 않으면 올림한다`(perSecond: Int, intervalMillis: Long) {
        val time = FakeTime()
        val limiter = limiter(mapOf(TossRateLimitGroup.MARKET_DATA to perSecond), time)

        limiter.acquire(TossRateLimitGroup.MARKET_DATA)
        limiter.acquire(TossRateLimitGroup.MARKET_DATA)
        limiter.acquire(TossRateLimitGroup.MARKET_DATA)

        assertEquals(listOf(intervalMillis, intervalMillis), time.slept)
    }

    @Test
    fun `첫 호출은 기다리지 않는다`() {
        val time = FakeTime()

        limiter(mapOf(TossRateLimitGroup.ACCOUNT to 1), time).acquire(TossRateLimitGroup.ACCOUNT)

        assertTrue(time.slept.isEmpty(), "slept=${time.slept}")
    }

    @Test
    fun `간격이 이미 지났으면 기다리지 않는다`() {
        val time = FakeTime()
        val limiter = limiter(mapOf(TossRateLimitGroup.ACCOUNT to 1), time)

        limiter.acquire(TossRateLimitGroup.ACCOUNT)
        time.clock.addAndGet(1_000)
        limiter.acquire(TossRateLimitGroup.ACCOUNT)

        assertTrue(time.slept.isEmpty(), "slept=${time.slept}")
    }

    @Test
    fun `간격이 일부만 지났으면 남은 시간만 기다린다`() {
        val time = FakeTime()
        val limiter = limiter(mapOf(TossRateLimitGroup.ACCOUNT to 1), time)

        limiter.acquire(TossRateLimitGroup.ACCOUNT)
        time.clock.addAndGet(400)
        limiter.acquire(TossRateLimitGroup.ACCOUNT)

        assertEquals(listOf(600L), time.slept)
    }

    @Test
    fun `그룹마다 따로 센다`() {
        val time = FakeTime()
        val limiter = limiter(
            mapOf(TossRateLimitGroup.ACCOUNT to 1, TossRateLimitGroup.ASSET to 5, TossRateLimitGroup.AUTH to 5),
            time,
        )

        limiter.acquire(TossRateLimitGroup.ACCOUNT)
        limiter.acquire(TossRateLimitGroup.ASSET)
        limiter.acquire(TossRateLimitGroup.AUTH)

        assertTrue(time.slept.isEmpty(), "다른 그룹은 서로 기다리지 않음: ${time.slept}")
    }

    @Test
    fun `한도는 설정값이라 같은 그룹도 설정을 바꾸면 간격이 바뀐다`() {
        val fast = FakeTime()
        val slow = FakeTime()

        listOf(fast to 20, slow to 2).forEach { (time, perSecond) ->
            val limiter = limiter(mapOf(TossRateLimitGroup.MARKET_DATA_CHART to perSecond), time)
            limiter.acquire(TossRateLimitGroup.MARKET_DATA_CHART)
            limiter.acquire(TossRateLimitGroup.MARKET_DATA_CHART)
        }

        assertEquals(listOf(50L), fast.slept)
        assertEquals(listOf(500L), slow.slept)
    }

    @Test
    fun `스레드 여러 개가 동시에 호출해도 슬롯이 겹치지 않고 간격만큼 벌어진다`() {
        val time = FakeTime()
        val limiter = TossRateLimiter(
            properties(mapOf(TossRateLimitGroup.ASSET to 5)),
            time.clock::get,
            // 시계를 움직이지 않고 기다릴 시간만 기록, 예약된 슬롯이 서로 다른지 본다
            { millis -> synchronized(time.slept) { time.slept += millis } },
        )
        val threadCount = 10
        val pool = Executors.newFixedThreadPool(threadCount)
        val ready = CountDownLatch(threadCount)
        val start = CountDownLatch(1)

        val futures = (0 until threadCount).map {
            pool.submit {
                ready.countDown()
                start.await()
                limiter.acquire(TossRateLimitGroup.ASSET)
            }
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS))
        start.countDown()
        futures.forEach { it.get(10, TimeUnit.SECONDS) }
        pool.shutdown()

        // 첫 호출은 기다리지 않고 나머지 9개는 200, 400 … 1800ms 슬롯
        assertEquals((1 until threadCount).map { it * 200L }, time.slept.sorted())
    }

    @Test
    fun `한도가 설정에 없는 그룹은 조용히 통과시키지 않고 환경변수 이름이 든 예외로 실패한다`() {
        val time = FakeTime()

        val error = assertThrows<TossIntegrationDisabledException> {
            limiter(mapOf(TossRateLimitGroup.AUTH to 5), time).acquire(TossRateLimitGroup.ACCOUNT)
        }

        assertEquals("TOSS_INTEGRATION_DISABLED", error.errorCode)
        assertTrue(error.message.orEmpty().contains("TOSS_RATE_LIMIT_ACCOUNT"), error.message)
    }

    @ParameterizedTest(name = "한도 {0} 은 범위 오류다")
    @CsvSource("0", "-3")
    fun `한도가 0 이하이면 0으로 나누지 않고 예외로 실패한다`(perSecond: Int) {
        val time = FakeTime()

        assertThrows<TossIntegrationDisabledException> {
            limiter(mapOf(TossRateLimitGroup.ASSET to perSecond), time).acquire(TossRateLimitGroup.ASSET)
        }
        assertTrue(time.slept.isEmpty())
    }

    // ---- C-01: 예약 전 대기가 최대 대기 상한을 넘으면 슬롯을 잡지 않고 TOSS_RATE_LIMITED ----

    // 시계를 움직이지 않고 기다릴 시간만 기록, 여러 스레드가 같은 시각에 몰린 상황
    private class StillClock {
        val clock = AtomicLong(1_000)
        val slept = mutableListOf<Long>()

        fun limiterFor(properties: TossProperties) = TossRateLimiter(properties, clock::get) { slept += it }
    }

    private fun capped(maxWaitMillis: Long, limits: Map<TossRateLimitGroup, Int>) = properties(limits)
        // devkit:test-change S6-04 줄 대기 상한이 429 예산 키에서 새 키 queue-wait 로 분리됨, 기대값 그대로
        .apply { rateLimitMaxQueueWaitMillis = maxWaitMillis }

    @Test
    fun `예약 전 대기가 최대 대기 상한을 넘으면 잠들지 않고 TOSS_RATE_LIMITED 이다`() {
        val still = StillClock()
        val limiter = still.limiterFor(capped(1_500, mapOf(TossRateLimitGroup.ACCOUNT to 1)))

        limiter.acquire(TossRateLimitGroup.ACCOUNT)
        limiter.acquire(TossRateLimitGroup.ACCOUNT)
        val error = assertThrows<TossRateLimitedException> { limiter.acquire(TossRateLimitGroup.ACCOUNT) }

        assertEquals("TOSS_RATE_LIMITED", error.errorCode)
        assertEquals(listOf(1_000L), still.slept, "첫 호출은 0, 둘째는 1000ms, 셋째는 2000ms 라 상한 1500ms 를 넘어 잠들지 않음")
    }

    @Test
    fun `상한을 넘겨 실패한 호출은 슬롯을 잡지 않아 다음 호출의 대기를 늘리지 않는다`() {
        val still = StillClock()
        val limiter = still.limiterFor(capped(1_500, mapOf(TossRateLimitGroup.ACCOUNT to 1)))
        limiter.acquire(TossRateLimitGroup.ACCOUNT)
        limiter.acquire(TossRateLimitGroup.ACCOUNT)
        assertThrows<TossRateLimitedException> { limiter.acquire(TossRateLimitGroup.ACCOUNT) }

        still.clock.addAndGet(600)
        limiter.acquire(TossRateLimitGroup.ACCOUNT)

        assertEquals(listOf(1_000L, 1_400L), still.slept, "실패한 호출이 슬롯을 잡았다면 둘째 대기가 2400ms 가 되어 상한을 넘음")
    }

    @Test
    fun `대기가 상한과 정확히 같으면 허용하고 상한 0 이면 기다려야 하는 호출은 실패한다`() {
        val exact = StillClock()
        val atLimit = exact.limiterFor(capped(1_000, mapOf(TossRateLimitGroup.ACCOUNT to 1)))
        atLimit.acquire(TossRateLimitGroup.ACCOUNT)
        atLimit.acquire(TossRateLimitGroup.ACCOUNT)
        assertEquals(listOf(1_000L), exact.slept)

        val zero = StillClock()
        val noWait = zero.limiterFor(capped(0, mapOf(TossRateLimitGroup.ACCOUNT to 1)))
        noWait.acquire(TossRateLimitGroup.ACCOUNT)
        assertThrows<TossRateLimitedException> { noWait.acquire(TossRateLimitGroup.ACCOUNT) }
        assertTrue(zero.slept.isEmpty())
    }

    // ---- C-05: 429 때 그 그룹의 다음 슬롯을 now + Retry-After 뒤로 밀어 모든 스레드가 같은 대기를 나눔 ----

    @Test
    fun `슬롯을 뒤로 밀면 다음 호출은 밀린 시간만큼 기다린다`() {
        val still = StillClock()
        val limiter = still.limiterFor(properties(mapOf(TossRateLimitGroup.ASSET to 5)))

        limiter.deferNextSlot(TossRateLimitGroup.ASSET, 3_000)
        limiter.acquire(TossRateLimitGroup.ASSET)

        assertEquals(listOf(3_000L), still.slept)
    }

    @Test
    fun `슬롯 미루기는 이미 더 뒤에 있는 슬롯을 앞당기지 않고 다른 그룹에 영향이 없다`() {
        val still = StillClock()
        val limiter = still.limiterFor(
            properties(mapOf(TossRateLimitGroup.ASSET to 5, TossRateLimitGroup.MARKET_DATA to 5)),
        )
        limiter.deferNextSlot(TossRateLimitGroup.ASSET, 4_000)

        limiter.deferNextSlot(TossRateLimitGroup.ASSET, 1_000)
        limiter.acquire(TossRateLimitGroup.MARKET_DATA)
        limiter.acquire(TossRateLimitGroup.ASSET)

        assertEquals(listOf(4_000L), still.slept)
    }

    @Test
    fun `슬롯 미루기 뒤 간격이 지나면 더 기다리지 않는다`() {
        val still = StillClock()
        val limiter = still.limiterFor(properties(mapOf(TossRateLimitGroup.ASSET to 5)))
        limiter.deferNextSlot(TossRateLimitGroup.ASSET, 2_000)

        still.clock.addAndGet(2_000)
        limiter.acquire(TossRateLimitGroup.ASSET)

        assertTrue(still.slept.isEmpty(), "${still.slept}")
    }

    // ---- S6-02: 이미 슬롯을 예약해 잠든 호출도 잠든 사이 받은 429 의 멈춤 끝 시각까지 더 잠 ----

    // 첫 잠이 시작될 때(다른 스레드가 429 를 받은 순간) 슬롯 미루기를 호출, 그 뒤 시계를 잠잔 만큼 움직임
    private fun deferDuringFirstSleep(delayMillis: Long): Pair<TossRateLimiter, List<Long>> {
        val clock = AtomicLong(1_000)
        val slept = mutableListOf<Long>()
        val holder = arrayOfNulls<TossRateLimiter>(1)
        var deferred = false
        val limiter = TossRateLimiter(properties(mapOf(TossRateLimitGroup.ASSET to 5)), clock::get) { millis ->
            slept += millis
            if (!deferred) {
                deferred = true
                holder[0]?.deferNextSlot(TossRateLimitGroup.ASSET, delayMillis)
            }
            clock.addAndGet(millis)
        }
        holder[0] = limiter
        return limiter to slept
    }

    @Test
    fun `이미 예약해 잠든 호출은 잠든 사이 받은 Retry-After 가 끝날 때까지 남은 만큼 더 잔다`() {
        val (limiter, slept) = deferDuringFirstSleep(1_000)

        limiter.acquire(TossRateLimitGroup.ASSET)
        limiter.acquire(TossRateLimitGroup.ASSET)

        // 둘째는 200ms 슬롯을 예약해 자고, 자는 사이(시각 1000) 받은 1000ms 멈춤이 시각 2000 에 끝나므로 깬 시각 1200 에서 800ms 를 더 잔다
        assertEquals(listOf(200L, 800L), slept)
    }

    @Test
    fun `잠든 사이 받은 Retry-After 가 깨기 전에 끝났으면 더 자지 않는다`() {
        val (limiter, slept) = deferDuringFirstSleep(100)

        limiter.acquire(TossRateLimitGroup.ASSET)
        limiter.acquire(TossRateLimitGroup.ASSET)

        assertEquals(listOf(200L), slept)
    }

    @Test
    fun `예약할 때 이미 있던 멈춤은 슬롯에 반영돼 있어 깬 뒤 다시 자지 않는다`() {
        val still = StillClock()
        val limiter = still.limiterFor(properties(mapOf(TossRateLimitGroup.ASSET to 5)))
        limiter.deferNextSlot(TossRateLimitGroup.ASSET, 1_000)

        limiter.acquire(TossRateLimitGroup.ASSET)

        // 시계가 움직이지 않아도(잠이 끝나도 시각이 그대로) 같은 멈춤으로 무한히 다시 자지 않음
        assertEquals(listOf(1_000L), still.slept)
    }

    // ---- S7-01: 멈춤 창 안에 예약해 잠든 호출은 깬 뒤 멈춤 끝 뒤의 새 슬롯을 받아 간격을 되찾음 ----

    @Test
    fun `멈춤 창 안에 예약해 잠든 호출들은 멈춤 끝부터 간격만큼 벌어져 나간다`() {
        // 스레드마다 시계를 따로 둠, 깬 시각이 스레드 스케줄링에 흔들리지 않게 (송신 시각 = 슬롯 시각)
        val threadClock = ThreadLocal.withInitial { 1_000L }
        val reserved = CountDownLatch(3)
        val pauseGiven = CountDownLatch(1)
        val limits = properties(mapOf(TossRateLimitGroup.MARKET_DATA to 10))
        val limiter = TossRateLimiter(limits, { threadClock.get() }) { millis ->
            reserved.countDown()
            check(pauseGiven.await(10, TimeUnit.SECONDS)) { "멈춤이 주어지지 않음" }
            threadClock.set(threadClock.get() + millis)
        }
        limiter.acquire(TossRateLimitGroup.MARKET_DATA)
        val pool = Executors.newFixedThreadPool(3)

        val sends = (1..3).map {
            pool.submit<Long> {
                limiter.acquire(TossRateLimitGroup.MARKET_DATA)
                threadClock.get()
            }
        }
        assertTrue(reserved.await(10, TimeUnit.SECONDS), "세 호출이 모두 슬롯을 예약해 잠들어야 함")
        limiter.deferNextSlot(TossRateLimitGroup.MARKET_DATA, 1_000)
        pauseGiven.countDown()
        val sendTimes = sends.map { it.get(10, TimeUnit.SECONDS) }.sorted()
        pool.shutdown()

        // 멈춤 끝 2000 부터 간격 100ms 씩, 한 순간에 몰리지 않음
        assertEquals(listOf(2_000L, 2_100L, 2_200L), sendTimes)
    }

    @Test
    fun `잠든 사이 늘어난 멈춤으로 다시 받은 슬롯이 줄 대기 상한을 넘으면 TOSS_RATE_LIMITED 이다`() {
        val clock = AtomicLong(1_000)
        val slept = mutableListOf<Long>()
        val holder = arrayOfNulls<TossRateLimiter>(1)
        val limiter = TossRateLimiter(capped(1_500, mapOf(TossRateLimitGroup.ACCOUNT to 1)), clock::get) { millis ->
            slept += millis
            if (slept.size == 1) holder[0]?.deferNextSlot(TossRateLimitGroup.ACCOUNT, 5_000)
            clock.addAndGet(millis)
        }
        holder[0] = limiter
        limiter.acquire(TossRateLimitGroup.ACCOUNT)

        // 둘째는 1000ms 슬롯을 예약해 상한 안에서 잠들고, 자는 사이 5000ms 멈춤이 걸려 깬 뒤 새 슬롯의 대기가 4000ms 라 상한을 넘음
        val error = assertThrows<TossRateLimitedException> { limiter.acquire(TossRateLimitGroup.ACCOUNT) }

        assertEquals("TOSS_RATE_LIMITED", error.errorCode)
        assertEquals(listOf(1_000L), slept, "상한을 넘는 늘어난 잠은 자지 않음")
    }

    // ---- S6-04: 줄 대기 상한은 새 키(rateLimitMaxQueueWaitMillis), 429 대기 예산 키와 따로 ----

    @Test
    fun `줄 대기 상한은 429 대기 예산 키가 0 이어도 새 키를 따른다`() {
        val still = StillClock()
        val props = properties(mapOf(TossRateLimitGroup.ACCOUNT to 1)).apply {
            rateLimitMaxWaitMillis = 0
            rateLimitMaxQueueWaitMillis = 1_500
        }
        val limiter = still.limiterFor(props)

        limiter.acquire(TossRateLimitGroup.ACCOUNT)
        limiter.acquire(TossRateLimitGroup.ACCOUNT)

        assertEquals(listOf(1_000L), still.slept)
    }

    @Test
    fun `새 키 상한을 넘으면 429 대기 예산 키가 커도 TOSS_RATE_LIMITED 이다`() {
        val still = StillClock()
        val props = properties(mapOf(TossRateLimitGroup.ACCOUNT to 1)).apply {
            rateLimitMaxWaitMillis = 60_000
            rateLimitMaxQueueWaitMillis = 1_500
        }
        val limiter = still.limiterFor(props)
        limiter.acquire(TossRateLimitGroup.ACCOUNT)
        limiter.acquire(TossRateLimitGroup.ACCOUNT)

        assertThrows<TossRateLimitedException> { limiter.acquire(TossRateLimitGroup.ACCOUNT) }
    }
}
