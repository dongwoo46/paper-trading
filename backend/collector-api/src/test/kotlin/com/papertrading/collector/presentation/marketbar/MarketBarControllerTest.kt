package com.papertrading.collector.presentation.marketbar

import com.papertrading.collector.application.marketbar.service.MarketBarQueryService
import com.papertrading.collector.domain.marketbar.MarketBar
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.Instant

class MarketBarControllerTest {
    private lateinit var queryService: MarketBarQueryService
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        queryService = mockk()
        val controller = MarketBarController(queryService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `GET bars 200 — 1m 기본 조회`() {
        every { queryService.getBars("005930", "1m", 60) } returns listOf(sampleBar("005930", "1m"))
        mockMvc.perform(
            get("/api/market/bars/005930")
                .queryParam("interval", "1m")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.symbol").value("005930"))
            .andExpect(jsonPath("$.interval").value("1m"))
            .andExpect(jsonPath("$.bars[0].tickCount").value(42))
    }

    @Test
    fun `GET bars 200 — limit 명시`() {
        every { queryService.getBars("005930", "5m", 10) } returns listOf(sampleBar("005930", "5m"))
        mockMvc.perform(
            get("/api/market/bars/005930")
                .queryParam("interval", "5m")
                .queryParam("limit", "10")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.bars.length()").value(1))
    }

    @Test
    fun `GET bars 400 — 잘못된 interval`() {
        every { queryService.getBars("005930", "3m", any()) } throws
            ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid interval: 3m")
        mockMvc.perform(
            get("/api/market/bars/005930")
                .queryParam("interval", "3m")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET bars 404 — 데이터 없음`() {
        every { queryService.getBars("005930", "1m", 60) } returns emptyList()
        mockMvc.perform(
            get("/api/market/bars/005930")
                .queryParam("interval", "1m")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET bars 200 — limit 100 초과 시 100으로 클램프`() {
        every { queryService.getBars("005930", "1m", 100) } returns listOf(sampleBar("005930", "1m"))
        mockMvc.perform(
            get("/api/market/bars/005930")
                .queryParam("interval", "1m")
                .queryParam("limit", "200")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
    }

    private fun sampleBar(symbol: String, interval: String) = MarketBar(
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
