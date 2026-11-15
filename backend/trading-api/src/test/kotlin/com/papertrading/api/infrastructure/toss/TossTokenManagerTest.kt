package com.papertrading.api.infrastructure.toss

import com.papertrading.api.common.exception.TossIntegrationDisabledException
import com.papertrading.api.common.exception.TossTokenUnavailableException
import com.papertrading.api.infrastructure.redis.RedisDistributedLock
import com.papertrading.api.infrastructure.security.AesGcmTokenCipher
import com.papertrading.api.support.SharedRedisContainer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

// 실제 Redis 컨테이너로 토큰 캐시·락·무효화를 확인 (PTT1, RULE-05, RULE-06)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TossTokenManagerTest {

    // 시험 전용 가짜 값
    private val keyBytes = ByteArray(32) { (it + 3).toByte() }
    private val encryptionKey = Base64.getEncoder().encodeToString(keyBytes)
    private val cipher = AesGcmTokenCipher(keyBytes)

    private val factory = SharedRedisContainer.newConnectionFactory()
    private val redisTemplate = SharedRedisContainer.newTemplate(factory)
    private val lock = RedisDistributedLock(redisTemplate)

    @BeforeEach
    fun clean() {
        redisTemplate.delete(listOf(TossTokenManager.TOKEN_KEY, TossTokenManager.LOCK_KEY))
    }

    @AfterAll
    fun close() {
        factory.destroy()
    }

    private fun properties(skewSeconds: Long = 60, lockWaitMillis: Long = 30_000) = TossProperties().apply {
        baseUrl = "https://toss.example.invalid"
        clientId = "fake-client-id"
        clientSecret = "fake-client-secret"
        tokenEncryptionKey = encryptionKey
        tokenRefreshSkewSeconds = skewSeconds
        tokenLockWaitMillis = lockWaitMillis
    }

    private fun manager(
        issuer: TossTokenIssuer,
        properties: TossProperties = properties(),
        nowMillis: () -> Long = System::currentTimeMillis,
        sleeper: (Long) -> Unit = Thread::sleep,
    ) = TossTokenManager(properties, redisTemplate, lock, issuer, nowMillis, sleeper)

    private fun rawToken(): String? = redisTemplate.opsForValue().get(TossTokenManager.TOKEN_KEY)

    @Test
    fun `인스턴스 2개와 스레드 16개가 빈 캐시에서 동시에 getToken 하면 발급은 정확히 1회이고 모두 같은 토큰이다`() {
        val issuer = FakeTossTokenIssuer(expiresInSeconds = 3_600, delayMillis = 300)
        val managers = listOf(manager(issuer), manager(issuer))
        val threadCount = 16
        val pool = Executors.newFixedThreadPool(threadCount)
        val ready = CountDownLatch(threadCount)
        val start = CountDownLatch(1)

        val futures = (0 until threadCount).map { index ->
            pool.submit<String> {
                ready.countDown()
                start.await()
                managers[index % managers.size].getToken()
            }
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS))
        start.countDown()
        val tokens = futures.map { it.get(30, TimeUnit.SECONDS) }
        pool.shutdown()

        assertEquals(1, issuer.callCount)
        assertEquals(1, tokens.toSet().size)
        assertEquals("fake-access-token-1", tokens.first())
        assertNull(redisTemplate.opsForValue().get(TossTokenManager.LOCK_KEY), "발급 뒤 락 해제")
    }

    @Test
    fun `Redis 원값은 v1 로 시작하는 암호문이고 평문 토큰을 담지 않으며 AAD 는 키 이름이다`() {
        val manager = manager(FakeTossTokenIssuer())

        val token = manager.getToken()

        val raw = requireNotNull(rawToken())
        assertTrue(raw.startsWith("v1."))
        assertFalse(raw.contains(token))
        assertEquals(token, cipher.decrypt(raw, TossTokenManager.TOKEN_KEY))
    }

    @Test
    fun `TTL 은 expires_in 에서 갱신 여유를 뺀 값이다`() {
        manager(FakeTossTokenIssuer(expiresInSeconds = 3_600), properties(skewSeconds = 600)).getToken()

        val ttl = requireNotNull(redisTemplate.getExpire(TossTokenManager.TOKEN_KEY, TimeUnit.SECONDS))
        assertTrue(ttl in 2_995..3_000, "ttl=$ttl")
    }

    @Test
    fun `캐시에 토큰이 있으면 발급하지 않고 락도 잡지 않는다`() {
        val issuer = FakeTossTokenIssuer()
        val manager = manager(issuer)
        val first = manager.getToken()
        val holder = requireNotNull(lock.tryAcquire(TossTokenManager.LOCK_KEY, 60_000))

        val second = manager.getToken()

        assertEquals(first, second)
        assertEquals(1, issuer.callCount)
        assertEquals(holder, redisTemplate.opsForValue().get(TossTokenManager.LOCK_KEY))
    }

    @Test
    fun `발급은 락을 쥔 동안에만 일어난다`() {
        var lockValueDuringIssue: String? = null
        val issuer = FakeTossTokenIssuer(onIssue = {
            lockValueDuringIssue = redisTemplate.opsForValue().get(TossTokenManager.LOCK_KEY)
        })

        manager(issuer).getToken()

        assertNotNull(lockValueDuringIssue, "발급 중에 락 키가 있어야 함(RULE-06)")
        assertNull(redisTemplate.opsForValue().get(TossTokenManager.LOCK_KEY))
    }

    @Test
    fun `락을 잡은 뒤 Redis 를 다시 확인해 다른 쪽이 저장한 토큰이 있으면 발급하지 않는다`() {
        val issuer = FakeTossTokenIssuer()
        val other = "다른-프로세스가-저장한-토큰"
        // 첫 확인 뒤 락 획득 사이에 다른 쪽이 저장하는 경합을 락 획득 직전 저장으로 재현
        val racingLock = object : RedisDistributedLock(redisTemplate) {
            override fun tryAcquire(key: String, ttlMillis: Long): String? {
                redisTemplate.opsForValue().set(
                    TossTokenManager.TOKEN_KEY,
                    cipher.encrypt(other, TossTokenManager.TOKEN_KEY),
                )
                return super.tryAcquire(key, ttlMillis)
            }
        }
        val manager = TossTokenManager(
            properties(),
            redisTemplate,
            racingLock,
            issuer,
            System::currentTimeMillis,
            Thread::sleep,
        )

        assertEquals(other, manager.getToken())
        assertEquals(0, issuer.callCount)
        assertNull(redisTemplate.opsForValue().get(TossTokenManager.LOCK_KEY), "락 해제")
    }

    @Test
    fun `락을 못 얻으면 대기 한도까지 토큰만 다시 읽고 다른 쪽이 저장하면 그 토큰을 쓴다`() {
        val issuer = FakeTossTokenIssuer()
        val holder = requireNotNull(lock.tryAcquire(TossTokenManager.LOCK_KEY, 60_000))
        val saved = "락-보유자가-저장한-토큰"
        var polls = 0
        val manager = manager(issuer, sleeper = {
            polls++
            if (polls == 3) {
                val encrypted = cipher.encrypt(saved, TossTokenManager.TOKEN_KEY)
                redisTemplate.opsForValue().set(TossTokenManager.TOKEN_KEY, encrypted)
            }
        })

        assertEquals(saved, manager.getToken())
        assertEquals(3, polls)
        assertEquals(0, issuer.callCount)
        assertEquals(holder, redisTemplate.opsForValue().get(TossTokenManager.LOCK_KEY), "남의 락은 그대로")
    }

    @Test
    fun `대기 한도를 넘기면 TOSS_TOKEN_UNAVAILABLE 이고 발급은 0회이며 남의 락은 그대로다`() {
        val issuer = FakeTossTokenIssuer()
        val holder = requireNotNull(lock.tryAcquire(TossTokenManager.LOCK_KEY, 60_000))
        val clock = AtomicLong(1_000)
        val slept = mutableListOf<Long>()
        val manager = manager(
            issuer,
            properties(lockWaitMillis = 500),
            nowMillis = clock::get,
            sleeper = { millis ->
                slept += millis
                clock.addAndGet(millis)
            },
        )

        val error = assertThrows(TossTokenUnavailableException::class.java) { manager.getToken() }

        assertEquals("TOSS_TOKEN_UNAVAILABLE", error.errorCode)
        assertEquals(0, issuer.callCount)
        assertTrue(slept.isNotEmpty() && slept.sum() >= 500, "대기 한도까지 가짜 시계가 흐름: $slept")
        assertEquals(holder, redisTemplate.opsForValue().get(TossTokenManager.LOCK_KEY))
        assertNull(rawToken())
    }

    @Test
    fun `복호화에 실패하는 캐시는 없는 것으로 보고 재발급해 덮어쓴다`() {
        val issuer = FakeTossTokenIssuer()
        val otherCipher = AesGcmTokenCipher(ByteArray(32) { 9 })
        val stale = otherCipher.encrypt("키-교체-전-토큰", TossTokenManager.TOKEN_KEY)
        redisTemplate.opsForValue().set(TossTokenManager.TOKEN_KEY, stale)

        val token = manager(issuer).getToken()

        assertEquals("fake-access-token-1", token)
        assertEquals(1, issuer.callCount)
        assertEquals(token, cipher.decrypt(requireNotNull(rawToken()), TossTokenManager.TOKEN_KEY))
    }

    @Test
    fun `형식이 깨진 캐시 값도 재발급으로 복구한다`() {
        redisTemplate.opsForValue().set(TossTokenManager.TOKEN_KEY, "평문-토큰-그대로")
        val issuer = FakeTossTokenIssuer()

        assertEquals("fake-access-token-1", manager(issuer).getToken())
        assertEquals(1, issuer.callCount)
    }

    @Test
    fun `발급기가 실패해도 락은 풀리고 아무것도 저장되지 않는다`() {
        val failing = object : TossTokenIssuer {
            override fun issue(): TossIssuedToken = error("발급 실패")
        }

        assertThrows(IllegalStateException::class.java) { manager(failing).getToken() }

        assertNull(redisTemplate.opsForValue().get(TossTokenManager.LOCK_KEY))
        assertNull(rawToken())
    }

    @Test
    fun `expires_in 이 갱신 여유 이하이면 저장하지 않고 TOSS_TOKEN_UNAVAILABLE 로 실패한다`() {
        listOf(60L, 30L, 0L).forEach { expiresIn ->
            val manager = manager(FakeTossTokenIssuer(expiresInSeconds = expiresIn), properties(skewSeconds = 60))

            val error = assertThrows(
                TossTokenUnavailableException::class.java,
                { manager.getToken() },
                "expires_in=$expiresIn",
            )

            assertEquals("TOSS_TOKEN_UNAVAILABLE", error.errorCode)
            assertNull(rawToken(), "expires_in=$expiresIn")
            assertNull(redisTemplate.opsForValue().get(TossTokenManager.LOCK_KEY))
        }
    }

    @Test
    fun `invalidate 는 Redis 값이 그 토큰일 때만 지운다`() {
        val manager = manager(FakeTossTokenIssuer())
        val token = manager.getToken()

        assertFalse(manager.invalidate("다른-토큰"), "다른 토큰이면 지우지 않음")
        assertNotNull(rawToken())

        assertTrue(manager.invalidate(token))
        assertNull(rawToken())
        assertFalse(manager.invalidate(token), "이미 없음")
    }

    @Test
    fun `invalidate 는 그 사이 다른 쪽이 새로 저장한 토큰을 지우지 않는다`() {
        val manager = manager(FakeTossTokenIssuer())
        val staleToken = manager.getToken()
        val renewed = "다른-프로세스가-재발급한-토큰"
        redisTemplate.opsForValue().set(TossTokenManager.TOKEN_KEY, cipher.encrypt(renewed, TossTokenManager.TOKEN_KEY))

        assertFalse(manager.invalidate(staleToken))

        assertEquals(renewed, manager.getToken())
    }

    @Test
    fun `invalidate 뒤 getToken 은 락을 잡고 1회 재발급한다`() {
        val issuer = FakeTossTokenIssuer()
        val manager = manager(issuer)
        val first = manager.getToken()

        manager.invalidate(first)
        val second = manager.getToken()

        assertEquals("fake-access-token-2", second)
        assertEquals(2, issuer.callCount)
    }

    @Test
    fun `토스 설정이 비활성이면 getToken 은 Redis 도 발급기도 건드리지 않고 TOSS_INTEGRATION_DISABLED 이다`() {
        val issuer = FakeTossTokenIssuer()
        val disabled = TossProperties()
        val manager = TossTokenManager(disabled, redisTemplate, lock, issuer, System::currentTimeMillis, Thread::sleep)

        val error = assertThrows(TossIntegrationDisabledException::class.java) { manager.getToken() }

        assertEquals("TOSS_INTEGRATION_DISABLED", error.errorCode)
        assertEquals(0, issuer.callCount)
        assertNull(rawToken())
        assertThrows(TossIntegrationDisabledException::class.java) { manager.invalidate("아무-토큰") }
    }
}
