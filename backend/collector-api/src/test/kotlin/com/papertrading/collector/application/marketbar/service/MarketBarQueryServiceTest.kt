package com.papertrading.collector.application.marketbar.service

import com.papertrading.collector.application.marketbar.port.MarketBarRepository
import com.papertrading.collector.domain.marketbar.MarketBar
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.Instant

class MarketBarQueryServiceTest {
    private lateinit var repository: MarketBarRepository
    private lateinit var service: MarketBarQueryService

    @BeforeEach
    fun setUp() {
        repository = mockk()
        service = MarketBarQueryService(repository)
    }

    @Test
    fun `유효한 1m 조회 — 결과 반환`() {
        every { repository.findBars("005930", "1m", 60) } returns listOf(bar("005930", "1m"))
        val result = service.getBars("005930", "1m", 60)
        assertEquals(1, result.size)
        assertEquals("1m", result[0].interval)
    }

    @Test
    fun `유효한 5m 조회 — 결과 반환`() {
        every { repository.findBars("005930", "5m", 20) } returns listOf(bar("005930", "5m"))
        val result = service.getBars("005930", "5m", 20)
        assertEquals(1, result.size)
        assertEquals("5m", result[0].interval)
    }

    @Test
    fun `잘못된 interval — 400 예외`() {
        val ex = assertThrows(ResponseStatusException::class.java) {
            service.getBars("005930", "3m", 60)
        }
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.statusCode.value())
    }

    @Test
    fun `limit 0 이하 — 400 예외`() {
        val ex = assertThrows(ResponseStatusException::class.java) {
            service.getBars("005930", "1m", 0)
        }
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.statusCode.value())
    }

    @Test
    fun `limit 100 초과 — 100으로 클램프 후 정상 조회`() {
        every { repository.findBars("005930", "1m", 100) } returns emptyList()
        val result = service.getBars("005930", "1m", 101)
        assertEquals(0, result.size)
    }

    private fun bar(symbol: String, interval: String) = MarketBar(
        symbol = symbol,
        interval = interval,
        startedAt = Instant.parse("2026-05-08T10:00:00Z"),
        open = BigDecimal("70000"),
        high = BigDecimal("70500"),
        low = BigDecimal("69500"),
        close = BigDecimal("70200"),
        volume = BigDecimal("1000"),
        tradeValue = BigDecimal("70200000"),
        vwap = BigDecimal("70200"),
        tickCount = 42,
    )
}
