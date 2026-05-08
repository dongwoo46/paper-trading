package com.papertrading.collector.application.marketfeature.service

import com.papertrading.collector.application.marketfeature.port.MarketFeatureStore
import com.papertrading.collector.domain.marketfeature.FeatureSnapshot
import com.papertrading.collector.domain.marketfeature.FeatureWindow
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.Instant

class MarketFeatureQueryServiceTest {
    private lateinit var store: MarketFeatureStore
    private lateinit var queryService: MarketFeatureQueryService

    @BeforeEach
    fun setUp() {
        store = mockk()
        queryService = MarketFeatureQueryService(store)
    }

    @Test
    fun `throws 404 when all requested snapshots are missing`() {
        every { store.loadSnapshot("005930", FeatureWindow.M1) } returns null
        every { store.loadSnapshot("005930", FeatureWindow.M5) } returns null

        val ex = assertThrows(ResponseStatusException::class.java) {
            queryService.getSnapshots("005930", setOf(FeatureWindow.M1, FeatureWindow.M5))
        }

        assertEquals(404, ex.statusCode.value())
    }

    @Test
    fun `throws 409 when some requested snapshots are missing`() {
        every { store.loadSnapshot("005930", FeatureWindow.M1) } returns snapshot(FeatureWindow.M1)
        every { store.loadSnapshot("005930", FeatureWindow.M5) } returns null

        val ex = assertThrows(ResponseStatusException::class.java) {
            queryService.getSnapshots("005930", setOf(FeatureWindow.M1, FeatureWindow.M5))
        }

        assertEquals(409, ex.statusCode.value())
    }

    @Test
    fun `returns snapshots in window order`() {
        every { store.loadSnapshot("005930", FeatureWindow.M1) } returns snapshot(FeatureWindow.M1)
        every { store.loadSnapshot("005930", FeatureWindow.M5) } returns snapshot(FeatureWindow.M5)
        every { store.loadSnapshot("005930", FeatureWindow.M10) } returns snapshot(FeatureWindow.M10)

        val snapshots = queryService.getSnapshots(
            symbol = "005930",
            windows = setOf(FeatureWindow.M1, FeatureWindow.M5, FeatureWindow.M10),
        )

        assertEquals(listOf(FeatureWindow.M1, FeatureWindow.M5, FeatureWindow.M10), snapshots.map { it.window })
    }

    private fun snapshot(window: FeatureWindow): FeatureSnapshot {
        return FeatureSnapshot(
            window = window,
            open = BigDecimal("100"),
            high = BigDecimal("101"),
            low = BigDecimal("99"),
            close = BigDecimal("100"),
            returnRate = BigDecimal("0.01"),
            volume = BigDecimal("10"),
            tradeValue = BigDecimal("1000"),
            vwap = BigDecimal("100"),
            buyVolume = BigDecimal("6"),
            sellVolume = BigDecimal("4"),
            tradeImbalance = BigDecimal("0.2"),
            tickCount = 3,
            startedAt = Instant.parse("2026-05-08T10:00:00Z"),
            updatedAt = Instant.parse("2026-05-08T10:00:30Z"),
        )
    }
}
