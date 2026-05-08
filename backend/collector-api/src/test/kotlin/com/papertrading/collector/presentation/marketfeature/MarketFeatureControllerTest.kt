package com.papertrading.collector.presentation.marketfeature

import com.papertrading.collector.application.marketfeature.service.MarketFeatureQueryService
import com.papertrading.collector.domain.marketfeature.FeatureSnapshot
import com.papertrading.collector.domain.marketfeature.FeatureWindow
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.Instant

class MarketFeatureControllerTest {
    private lateinit var queryService: MarketFeatureQueryService
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        queryService = mockk()
        val controller = MarketFeatureController(queryService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `GET market features returns 200 with default windows`() {
        every {
            queryService.getSnapshots("005930", setOf(FeatureWindow.M1, FeatureWindow.M5, FeatureWindow.M10))
        } returns listOf(snapshot(FeatureWindow.M1), snapshot(FeatureWindow.M5), snapshot(FeatureWindow.M10))

        mockMvc.perform(get("/api/market/features/005930").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.symbol").value("005930"))
            .andExpect(jsonPath("$.features[0].window").value("1m"))
            .andExpect(jsonPath("$.features[1].window").value("5m"))
            .andExpect(jsonPath("$.features[2].window").value("10m"))
    }

    @Test
    fun `GET market features returns 400 for invalid symbol`() {
        mockMvc.perform(get("/api/market/features/00 5930").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET market features returns 400 for invalid windows`() {
        mockMvc.perform(
            get("/api/market/features/005930")
                .queryParam("windows", "1m,3m")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET market features returns 404 when snapshot not found`() {
        every {
            queryService.getSnapshots("005930", setOf(FeatureWindow.M1))
        } throws ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "not found")

        mockMvc.perform(
            get("/api/market/features/005930")
                .queryParam("windows", "1m")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET market features returns 409 on rollover race`() {
        every {
            queryService.getSnapshots("005930", setOf(FeatureWindow.M1))
        } throws ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "inconsistent")

        mockMvc.perform(
            get("/api/market/features/005930")
                .queryParam("windows", "1m")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isConflict)
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
