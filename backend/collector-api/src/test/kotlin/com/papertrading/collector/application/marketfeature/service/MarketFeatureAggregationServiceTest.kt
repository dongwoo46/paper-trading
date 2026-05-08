package com.papertrading.collector.application.marketfeature.service

import com.papertrading.collector.application.marketfeature.port.MarketFeatureStore
import com.papertrading.collector.domain.entity.kis.KisQuoteEvent
import com.papertrading.collector.domain.marketfeature.FeatureWindow
import com.papertrading.collector.domain.marketfeature.MinuteBarState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

class MarketFeatureAggregationServiceTest {
    private val store = mockk<MarketFeatureStore>()
    private val service = MarketFeatureAggregationService(store)

    @Test
    fun `onTick - 신규 minute bar 생성 및 snapshot 저장`() {
        every { store.loadCurrent("005930") } returns null
        every { store.saveCurrent("005930", any()) } just runs
        every { store.loadRecentBars("005930", 10) } returns emptyList()
        every { store.saveSnapshot("005930", any(), any()) } just runs

        service.onTick(event(price = "70000", volume = "100", at = "2026-05-08T11:30:05Z"))

        val savedCurrent = slot<MinuteBarState>()
        verify { store.saveCurrent("005930", capture(savedCurrent)) }
        assert(savedCurrent.captured.open == BigDecimal("70000"))
        assert(savedCurrent.captured.close == BigDecimal("70000"))
        verify(exactly = 3) { store.saveSnapshot("005930", any(), any()) }
    }

    @Test
    fun `onTick - 분 롤오버시 이전 bar append 후 current 교체`() {
        every { store.loadCurrent("005930") } returns MinuteBarState(
            minute = "2026-05-08T11:30:00Z",
            open = BigDecimal("70000"),
            high = BigDecimal("70100"),
            low = BigDecimal("69900"),
            close = BigDecimal("70050"),
            volume = BigDecimal("300"),
            tradeValue = BigDecimal("21000000"),
            buyVolume = BigDecimal("0"),
            sellVolume = BigDecimal("0"),
            tickCount = 3,
            startedAt = Instant.parse("2026-05-08T11:30:00Z"),
            updatedAt = Instant.parse("2026-05-08T11:30:50Z"),
        )
        every { store.appendBar("005930", any()) } just runs
        every { store.saveCurrent("005930", any()) } just runs
        every { store.loadRecentBars("005930", 10) } returns emptyList()
        every { store.saveSnapshot("005930", any(), any()) } just runs

        service.onTick(event(price = "70200", volume = "500", at = "2026-05-08T11:31:01Z"))

        verify(exactly = 1) { store.appendBar("005930", any()) }
        verify(exactly = 1) { store.saveCurrent("005930", any()) }
    }

    @Test
    fun `onTick - 5m 10m snapshot 계산`() {
        every { store.loadCurrent("005930") } returns null
        every { store.saveCurrent("005930", any()) } just runs
        every { store.loadRecentBars("005930", 10) } returns listOf(
            bar("2026-05-08T11:21:00Z", "68000", "68500", "67900", "68400", "100"),
            bar("2026-05-08T11:22:00Z", "68400", "68600", "68300", "68500", "110"),
            bar("2026-05-08T11:23:00Z", "68500", "68700", "68400", "68600", "120"),
            bar("2026-05-08T11:24:00Z", "68600", "68800", "68500", "68700", "130"),
            bar("2026-05-08T11:25:00Z", "68700", "68900", "68600", "68800", "140"),
            bar("2026-05-08T11:26:00Z", "68800", "69000", "68700", "68900", "150"),
            bar("2026-05-08T11:27:00Z", "68900", "69100", "68800", "69000", "160"),
            bar("2026-05-08T11:28:00Z", "69000", "69200", "68900", "69100", "170"),
            bar("2026-05-08T11:29:00Z", "69100", "69300", "69000", "69200", "180"),
        )
        every { store.saveSnapshot("005930", any(), any()) } just runs

        service.onTick(event(price = "69300", volume = "190", at = "2026-05-08T11:30:01Z"))

        verify { store.saveSnapshot("005930", FeatureWindow.M5, any()) }
        verify { store.saveSnapshot("005930", FeatureWindow.M10, any()) }
    }

    @Test
    fun `onTick - vwap and tradeImbalance 계산식 검증`() {
        every { store.loadCurrent("005930") } returns null
        every { store.saveCurrent("005930", any()) } just runs
        every { store.loadRecentBars("005930", 10) } returns listOf(
            bar("2026-05-08T11:29:00Z", "100", "100", "100", "100", "10", "7", "3"),
        )
        every { store.saveSnapshot("005930", any(), any()) } just runs

        service.onTick(event(price = "200", volume = "20", at = "2026-05-08T11:30:01Z"))

        val snapshotSlot = slot<com.papertrading.collector.domain.marketfeature.FeatureSnapshot>()
        verify { store.saveSnapshot("005930", FeatureWindow.M5, capture(snapshotSlot)) }

        val saved = snapshotSlot.captured
        assertEquals(BigDecimal("166.66666667"), saved.vwap)
        assertEquals(BigDecimal("0.13333333"), saved.tradeImbalance)
    }

    @Test
    fun `onTick - 분 경계 동시 tick 에서 appendBar 는 한번만 호출된다`() {
        val concurrentStore = ConcurrentStoreForRolloverRace(
            initialCurrent = bar("2026-05-08T11:30:00Z", "70000", "70100", "69900", "70050", "300"),
        )
        val concurrentService = MarketFeatureAggregationService(concurrentStore)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val done = CountDownLatch(2)
        val errors = ConcurrentLinkedQueue<Throwable>()

        repeat(2) {
            Thread {
                try {
                    ready.countDown()
                    start.await()
                    concurrentService.onTick(event(price = "70200", volume = "100", at = "2026-05-08T11:31:01Z"))
                } catch (t: Throwable) {
                    errors.add(t)
                } finally {
                    done.countDown()
                }
            }.start()
        }

        ready.await()
        start.countDown()
        done.await()

        assertEquals(0, errors.size)
        assertEquals(1, concurrentStore.appendCount.get())
    }

    private fun event(price: String, volume: String, at: String): KisQuoteEvent = KisQuoteEvent(
        ticker = "005930",
        price = BigDecimal(price),
        askp1 = BigDecimal(price),
        bidp1 = BigDecimal(price),
        high = BigDecimal(price),
        low = BigDecimal(price),
        volume = BigDecimal(volume),
        receivedAt = Instant.parse(at),
    )

    private fun bar(
        minute: String,
        open: String,
        high: String,
        low: String,
        close: String,
        volume: String,
        buyVolume: String = "0",
        sellVolume: String = "0",
    ): MinuteBarState =
        MinuteBarState(
            minute = minute,
            open = BigDecimal(open),
            high = BigDecimal(high),
            low = BigDecimal(low),
            close = BigDecimal(close),
            volume = BigDecimal(volume),
            tradeValue = BigDecimal(close) * BigDecimal(volume),
            buyVolume = BigDecimal(buyVolume),
            sellVolume = BigDecimal(sellVolume),
            tickCount = 1,
            startedAt = Instant.parse(minute),
            updatedAt = Instant.parse(minute),
        )

    private class ConcurrentStoreForRolloverRace(
        initialCurrent: MinuteBarState,
    ) : MarketFeatureStore {
        @Volatile
        private var current: MinuteBarState? = initialCurrent
        val appendCount = AtomicInteger(0)

        override fun loadCurrent(symbol: String): MinuteBarState? = current

        override fun saveCurrent(symbol: String, state: MinuteBarState) {
            current = state
        }

        override fun appendBar(symbol: String, bar: MinuteBarState) {
            appendCount.incrementAndGet()
        }

        override fun loadRecentBars(symbol: String, limit: Int): List<MinuteBarState> = emptyList()

        override fun saveSnapshot(
            symbol: String,
            window: FeatureWindow,
            snapshot: com.papertrading.collector.domain.marketfeature.FeatureSnapshot,
        ) = Unit

        override fun loadSnapshot(
            symbol: String,
            window: FeatureWindow,
        ): com.papertrading.collector.domain.marketfeature.FeatureSnapshot? = null
    }
}

