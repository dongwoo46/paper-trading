package com.papertrading.collector.application.market.service

import com.papertrading.collector.infra.market.persistence.MarketWeeklyOhlcvRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MarketWeeklyOhlcvQueryServiceTest {
	private lateinit var repository: MarketWeeklyOhlcvRepository
	private lateinit var service: MarketWeeklyOhlcvQueryService

	@BeforeEach
	fun setUp() {
		repository = mockk()
		service = MarketWeeklyOhlcvQueryService(repository)
	}

	@Test
	fun `blank symbol returns empty without repository call`() {
		val result = service.weeklyBars("yfinance", "   ", null, null, 260)
		assertTrue(result.isEmpty())
		verify(exactly = 0) { repository.findBySourceAndSymbolAndTradeDateBetweenOrderByTradeDateAsc(any(), any(), any(), any()) }
	}

	@Test
	fun `limit is clamped to 520 and source symbol are normalized`() {
		val from = LocalDate.parse("2024-01-01")
		val to = LocalDate.parse("2024-12-31")
		every {
			repository.findBySourceAndSymbolAndTradeDateBetweenOrderByTradeDateAsc(
				"yfinance",
				"AAPL",
				from,
				to,
			)
		} returns emptyList()

		val result = service.weeklyBars("YFINANCE", " aapl ", from, to, 9999)
		assertTrue(result.isEmpty())
		verify(exactly = 1) {
			repository.findBySourceAndSymbolAndTradeDateBetweenOrderByTradeDateAsc(
				"yfinance",
				"AAPL",
				from,
				to,
			)
		}
	}
}
