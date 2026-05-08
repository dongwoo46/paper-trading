package com.papertrading.collector.presentation.market

import com.papertrading.collector.application.market.service.InsufficientBarsForRequestedRangeException
import com.papertrading.collector.application.market.service.MarketIndicatorsQueryService
import com.papertrading.collector.application.market.service.MarketIndicatorsRange
import com.papertrading.collector.application.market.service.MarketIndicatorsResult
import com.papertrading.collector.application.market.service.SymbolNotFoundOrNoBarsException
import com.papertrading.collector.domain.market.indicator.IndicatorPoint
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

class MarketIndicatorsControllerTest {
	private lateinit var queryService: MarketIndicatorsQueryService
	private lateinit var mockMvc: MockMvc

	@BeforeEach
	fun setUp() {
		queryService = mockk()
		mockMvc = MockMvcBuilders.standaloneSetup(MarketIndicatorsController(queryService))
			.setControllerAdvice(GlobalExceptionHandler())
			.build()
	}

	@Test
	fun `GET indicators 200`() {
		every { queryService.query(any()) } returns MarketIndicatorsResult(
			symbol = "005930",
			interval = "1m",
			range = MarketIndicatorsRange(
				from = Instant.parse("2026-05-08T00:00:00Z"),
				to = Instant.parse("2026-05-08T00:00:00Z"),
				requestedLimit = 30,
				actualCount = 1,
			),
			points = listOf(IndicatorPoint(Instant.parse("2026-05-08T00:00:00Z"), BigDecimal("100"), null, null, null)),
		)
		mockMvc.perform(
			get("/api/market/indicators/005930")
				.queryParam("interval", "1m")
				.queryParam("limit", "30")
				.queryParam("indicators", "bb,rsi")
				.accept(MediaType.APPLICATION_JSON),
		).andExpect(status().isOk)
			.andExpect(jsonPath("$.symbol").value("005930"))
			.andExpect(jsonPath("$.range.requestedLimit").value(30))
			.andExpect(jsonPath("$.meta.missingPolicy").value("null_until_window_ready"))
			.andExpect(jsonPath("$.series.length()").value(1))
	}

	@Test
	fun `GET indicators 200 - 일봉 date 포맷 허용`() {
		every { queryService.query(any()) } returns MarketIndicatorsResult(
			symbol = "005930",
			interval = "1d",
			range = MarketIndicatorsRange(
				from = Instant.parse("2026-05-01T00:00:00Z"),
				to = Instant.parse("2026-05-08T00:00:00Z"),
				requestedLimit = null,
				actualCount = 1,
			),
			points = listOf(IndicatorPoint(Instant.parse("2026-05-08T00:00:00Z"), BigDecimal("100"), null, null, null)),
		)
		mockMvc.perform(
			get("/api/market/indicators/005930")
				.queryParam("interval", "1d")
				.queryParam("from", "2026-05-01")
				.queryParam("to", "2026-05-08")
				.queryParam("indicators", "bb"),
		).andExpect(status().isOk)
	}

	@Test
	fun `GET indicators 400 - from 형식 오류`() {
		mockMvc.perform(
			get("/api/market/indicators/005930")
				.queryParam("interval", "1m")
				.queryParam("from", "bad-date")
				.queryParam("to", "2026-05-08T00:00:00Z")
				.queryParam("indicators", "bb"),
		).andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("INVALID_PERIOD_QUERY"))
	}

	@Test
	fun `GET indicators 400 - interval 오류 코드`() {
		every { queryService.query(any()) } throws IllegalArgumentException("INVALID_INTERVAL")
		mockMvc.perform(
			get("/api/market/indicators/005930")
				.queryParam("interval", "15m")
				.queryParam("limit", "30")
				.queryParam("indicators", "bb"),
		).andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("INVALID_INTERVAL"))
	}

	@Test
	fun `GET indicators 404 - symbol no bars`() {
		every { queryService.query(any()) } throws SymbolNotFoundOrNoBarsException()
		mockMvc.perform(
			get("/api/market/indicators/UNKNOWN")
				.queryParam("interval", "1m")
				.queryParam("limit", "30")
				.queryParam("indicators", "bb"),
		).andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code").value("SYMBOL_NOT_FOUND_OR_NO_BARS"))
	}

	@Test
	fun `GET indicators 422 - insufficient bars`() {
		every { queryService.query(any()) } throws InsufficientBarsForRequestedRangeException()
		mockMvc.perform(
			get("/api/market/indicators/005930")
				.queryParam("interval", "1m")
				.queryParam("limit", "500")
				.queryParam("indicators", "bb"),
		).andExpect(status().isUnprocessableEntity)
			.andExpect(jsonPath("$.code").value("INSUFFICIENT_BARS_FOR_REQUESTED_RANGE"))
	}
}
