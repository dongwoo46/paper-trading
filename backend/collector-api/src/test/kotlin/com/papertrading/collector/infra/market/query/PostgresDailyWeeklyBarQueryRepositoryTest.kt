package com.papertrading.collector.infra.market.query

import com.papertrading.collector.application.market.service.MarketIndicatorsQuery
import com.papertrading.collector.domain.entity.market.MarketDailyOhlcv
import com.papertrading.collector.domain.market.indicator.Interval
import com.papertrading.collector.infra.market.persistence.MarketDailyOhlcvRepository
import com.papertrading.collector.infra.market.persistence.MarketWeeklyOhlcvRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class PostgresDailyWeeklyBarQueryRepositoryTest {
	@Test
	fun `1d limit 경로는 desc 조회 후 asc 정렬`() {
		val dailyRepo = mockk<MarketDailyOhlcvRepository>()
		val weeklyRepo = mockk<MarketWeeklyOhlcvRepository>()
		every { dailyRepo.findBySourceAndSymbolOrderByTradeDateDesc(any(), any(), any<Pageable>()) } returns listOf(
			daily("2026-05-03", "103"),
			daily("2026-05-02", "102"),
		)
		val repository = PostgresDailyWeeklyBarQueryRepository(dailyRepo, weeklyRepo)

		val result = repository.load("005930", Interval.ONE_DAY, baseRequest().copy(interval = "1d", limit = 2))

		assertEquals("102", result[0].close.toPlainString())
		assertEquals("103", result[1].close.toPlainString())
	}

	@Test
	fun `1w source 비어있으면 daily로 주봉 구성 fallback`() {
		val dailyRepo = mockk<MarketDailyOhlcvRepository>()
		val weeklyRepo = mockk<MarketWeeklyOhlcvRepository>()
		every { weeklyRepo.findBySourceAndSymbolAndTradeDateBetweenOrderByTradeDateAsc(any(), any(), any(), any()) } returns emptyList()
		every { dailyRepo.findBySourceAndSymbolAndTradeDateBetweenOrderByTradeDateAsc(any(), any(), any(), any()) } returns listOf(
			daily("2026-05-05", "101"),
			daily("2026-05-06", "102"),
			daily("2026-05-12", "201"),
		)
		val repository = PostgresDailyWeeklyBarQueryRepository(dailyRepo, weeklyRepo)

		val result = repository.load(
			"005930",
			Interval.ONE_WEEK,
			baseRequest().copy(interval = "1w", limit = null, from = Instant.parse("2026-05-01T00:00:00Z"), to = Instant.parse("2026-05-20T00:00:00Z")),
		)

		assertEquals(2, result.size)
		assertEquals("102", result[0].close.toPlainString())
		assertEquals("201", result[1].close.toPlainString())
		verify { dailyRepo.findBySourceAndSymbolAndTradeDateBetweenOrderByTradeDateAsc(any(), any(), any(), any()) }
	}

	@Test
	fun `1w limit 경로에서 weekly 비어있으면 daily fallback 후 limit 만큼 최근 주봉 반환`() {
		val dailyRepo = mockk<MarketDailyOhlcvRepository>()
		val weeklyRepo = mockk<MarketWeeklyOhlcvRepository>()
		every { weeklyRepo.findBySourceAndSymbolOrderByTradeDateDesc(any(), any(), any<Pageable>()) } returns emptyList()
		every { dailyRepo.findBySourceAndSymbolOrderByTradeDateDesc(any(), any(), any<Pageable>()) } returns listOf(
			daily("2026-05-28", "502"),
			daily("2026-05-27", "501"),
			daily("2026-05-21", "402"),
			daily("2026-05-20", "401"),
			daily("2026-05-14", "302"),
			daily("2026-05-13", "301"),
			daily("2026-05-07", "202"),
			daily("2026-05-06", "201"),
			daily("2026-04-30", "102"),
			daily("2026-04-29", "101"),
		)
		val repository = PostgresDailyWeeklyBarQueryRepository(dailyRepo, weeklyRepo)

		val result = repository.load("005930", Interval.ONE_WEEK, baseRequest().copy(interval = "1w", limit = 2))

		assertEquals(2, result.size)
		assertEquals("402", result[0].close.toPlainString())
		assertEquals("502", result[1].close.toPlainString())
		verify { weeklyRepo.findBySourceAndSymbolOrderByTradeDateDesc(any(), any(), any<Pageable>()) }
		verify { dailyRepo.findBySourceAndSymbolOrderByTradeDateDesc(any(), any(), any<Pageable>()) }
	}

	private fun baseRequest() = MarketIndicatorsQuery(
		symbol = "005930",
		interval = "1d",
		limit = 30,
		from = null,
		to = null,
		indicators = "bb",
		bbPeriod = null,
		bbStdDev = null,
		rsiPeriod = null,
		macdFast = null,
		macdSlow = null,
		macdSignal = null,
	)

	private fun daily(date: String, close: String): MarketDailyOhlcv {
		val daily = mockk<MarketDailyOhlcv>()
		every { daily.tradeDate } returns LocalDate.parse(date)
		every { daily.closePrice } returns BigDecimal(close)
		return daily
	}
}
