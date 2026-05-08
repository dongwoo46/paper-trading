package com.papertrading.collector.presentation.market

import com.papertrading.collector.application.market.service.InsufficientDataForRsException
import com.papertrading.collector.application.market.service.MarketMicrostructureQueryService
import com.papertrading.collector.application.market.service.MarketMicrostructureRange
import com.papertrading.collector.application.market.service.MarketMicrostructureResult
import com.papertrading.collector.application.market.service.RelativeStrengthBaseline
import com.papertrading.collector.application.market.service.SymbolNotFoundOrNoDataException
import com.papertrading.collector.domain.market.analytics.MarketMicrostructureSnapshot
import com.papertrading.collector.domain.market.analytics.RelativeStrengthPoint
import com.papertrading.collector.presentation.common.GlobalExceptionHandler
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
import java.math.BigDecimal
import java.time.Instant

class MarketMicrostructureControllerTest {
	private lateinit var queryService: MarketMicrostructureQueryService
	private lateinit var mockMvc: MockMvc

	@BeforeEach
	fun setUp() {
		queryService = mockk()
		mockMvc = MockMvcBuilders.standaloneSetup(MarketMicrostructureController(queryService))
			.setControllerAdvice(GlobalExceptionHandler())
			.build()
	}

	@Test
	fun `GET microstructure 200`() {
		every { queryService.query(any()) } returns sampleResult()
		mockMvc.perform(
			get("/api/market/microstructure/005930")
				.queryParam("interval", "1m")
				.queryParam("limit", "2")
				.accept(MediaType.APPLICATION_JSON),
		).andExpect(status().isOk)
			.andExpect(jsonPath("$.symbol").value("005930"))
			.andExpect(jsonPath("$.session").value("regular"))
			.andExpect(jsonPath("$.relativeStrength.series.length()").value(2))
	}

	@Test
	fun `GET microstructure 400 invalid session`() {
		every { queryService.query(any()) } throws IllegalArgumentException("INVALID_SESSION")
		mockMvc.perform(
			get("/api/market/microstructure/005930")
				.queryParam("interval", "1m")
				.queryParam("session", "overnight")
				.queryParam("limit", "2"),
		).andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("INVALID_SESSION"))
	}

	@Test
	fun `GET microstructure 404`() {
		every { queryService.query(any()) } throws SymbolNotFoundOrNoDataException()
		mockMvc.perform(
			get("/api/market/microstructure/UNKNOWN")
				.queryParam("interval", "1m")
				.queryParam("limit", "2"),
		).andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code").value("SYMBOL_NOT_FOUND_OR_NO_DATA"))
	}

	@Test
	fun `GET microstructure 422`() {
		every { queryService.query(any()) } throws InsufficientDataForRsException()
		mockMvc.perform(
			get("/api/market/microstructure/005930")
				.queryParam("interval", "1m")
				.queryParam("limit", "2"),
		).andExpect(status().isUnprocessableEntity)
			.andExpect(jsonPath("$.code").value("INSUFFICIENT_DATA_FOR_RS"))
	}

	private fun sampleResult() = MarketMicrostructureResult(
		symbol = "005930",
		interval = "1m",
		session = "regular",
		timezone = "Asia/Seoul",
		range = MarketMicrostructureRange(
			from = Instant.parse("2026-05-01T00:00:00Z"),
			to = Instant.parse("2026-05-01T00:01:00Z"),
			requestedLimit = 2,
			actualCount = 2,
		),
		microstructure = MarketMicrostructureSnapshot(
			bestBid = null,
			bestAsk = null,
			spread = null,
			bidAskImbalance = BigDecimal("0.2"),
			bidDepthTopN = null,
			askDepthTopN = null,
			depthImbalance = null,
			buyVolume = BigDecimal("600"),
			sellVolume = BigDecimal("400"),
			tradeIntensity = BigDecimal("0.2"),
			vwap = BigDecimal("100"),
			rvol = BigDecimal("1000"),
			timestamp = Instant.parse("2026-05-01T00:01:00Z"),
		),
		relativeStrengthBaseline = RelativeStrengthBaseline(benchmark = "KOSPI200", sector = null),
		relativeStrengthSeries = listOf(
			RelativeStrengthPoint(
				timestamp = Instant.parse("2026-05-01T00:00:00Z"),
				ratio = BigDecimal("0.50000000"),
				returnDelta = BigDecimal("0E-8"),
			),
			RelativeStrengthPoint(
				timestamp = Instant.parse("2026-05-01T00:01:00Z"),
				ratio = BigDecimal("0.52380952"),
				returnDelta = BigDecimal("0.05000000"),
			),
		),
		warnings = emptyList(),
	)
}
