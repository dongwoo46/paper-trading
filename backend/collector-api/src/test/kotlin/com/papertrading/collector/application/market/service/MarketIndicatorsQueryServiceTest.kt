package com.papertrading.collector.application.market.service

import com.papertrading.collector.domain.market.indicator.BollingerBandsCalculator
import com.papertrading.collector.domain.market.indicator.Interval
import com.papertrading.collector.domain.market.indicator.MacdCalculator
import com.papertrading.collector.domain.market.indicator.RsiCalculator
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class MarketIndicatorsQueryServiceTest {
	@Test
	fun `period 정책 위반(limit와 from-to 동시) 시 실패`() {
		val service = serviceWithSource(mockk())
		assertThrows(IllegalArgumentException::class.java) {
			service.query(
				baseQuery().copy(
					limit = 10,
					from = Instant.parse("2026-05-01T00:00:00Z"),
					to = Instant.parse("2026-05-02T00:00:00Z"),
				),
			)
		}
	}

	@Test
	fun `멀티 인디케이터는 동일 타임스탬프 길이로 정렬`() {
		val source = mockk<MarketBarSource>()
		every { source.load(any(), any(), any()) } returns sampleBars(30)
		val resolver = mockk<MarketBarSourceResolver>()
		every { resolver.resolve(any()) } returns source

		val service = MarketIndicatorsQueryService(resolver, BollingerBandsCalculator(), RsiCalculator(), MacdCalculator())
		val result = service.query(baseQuery().copy(indicators = "bb,rsi,macd"))

		assertEquals(30, result.points.size)
		assertEquals(result.points.map { it.timestamp }.distinct().size, result.points.size)
		assertEquals("null_until_window_ready", result.meta.missingPolicy)
	}

	@Test
	fun `interval switch 동작 검증`() {
		val redisSource = mockk<MarketBarSource>()
		val pgSource = mockk<MarketBarSource>()
		every { redisSource.load(any(), any(), any()) } returns sampleBars(30)
		every { pgSource.load(any(), any(), any()) } returns sampleBars(30)
		val resolver = mockk<MarketBarSourceResolver>()
		every { resolver.resolve(Interval.ONE_MINUTE) } returns redisSource
		every { resolver.resolve(Interval.FIVE_MINUTES) } returns redisSource
		every { resolver.resolve(Interval.TEN_MINUTES) } returns redisSource
		every { resolver.resolve(Interval.ONE_DAY) } returns pgSource
		every { resolver.resolve(Interval.ONE_WEEK) } returns pgSource
		val service = MarketIndicatorsQueryService(resolver, BollingerBandsCalculator(), RsiCalculator(), MacdCalculator())

		service.query(baseQuery().copy(interval = "1m"))
		service.query(baseQuery().copy(interval = "5m"))
		service.query(baseQuery().copy(interval = "10m"))
		service.query(baseQuery().copy(interval = "1d", limit = null, from = Instant.parse("2026-05-01T00:00:00Z"), to = Instant.parse("2026-05-08T00:00:00Z")))
		service.query(baseQuery().copy(interval = "1w", limit = null, from = Instant.parse("2026-04-01T00:00:00Z"), to = Instant.parse("2026-05-08T00:00:00Z")))

		verify(exactly = 3) { redisSource.load(any(), any(), any()) }
		verify(exactly = 2) { pgSource.load(any(), any(), any()) }
	}

	@Test
	fun `limit과 from-to 모두 없으면 기본 limit 200 적용`() {
		val source = mockk<MarketBarSource>()
		val captured = slot<MarketIndicatorsQuery>()
		every { source.load(any(), any(), capture(captured)) } returns sampleBars(200)
		val resolver = mockk<MarketBarSourceResolver>()
		every { resolver.resolve(any()) } returns source
		val service = MarketIndicatorsQueryService(resolver, BollingerBandsCalculator(), RsiCalculator(), MacdCalculator())

		service.query(baseQuery().copy(limit = null, from = null, to = null))

		assertEquals(200, captured.captured.limit)
	}

	@Test
	fun `bar 데이터가 비어있으면 404 예외`() {
		val source = mockk<MarketBarSource>()
		every { source.load(any(), any(), any()) } returns emptyList()
		val service = serviceWithSource(source)

		assertThrows(SymbolNotFoundOrNoBarsException::class.java) {
			service.query(baseQuery())
		}
	}

	@Test
	fun `요청 limit보다 bar가 적으면 422 예외`() {
		val source = mockk<MarketBarSource>()
		every { source.load(any(), any(), any()) } returns sampleBars(10)
		val service = serviceWithSource(source)

		assertThrows(InsufficientBarsForRequestedRangeException::class.java) {
			service.query(baseQuery().copy(limit = 30))
		}
	}

	@Test
	fun `응답 range 필드가 bar 범위를 반영`() {
		val source = mockk<MarketBarSource>()
		every { source.load(any(), any(), any()) } returns sampleBars(3)
		val service = serviceWithSource(source)

		val result = service.query(baseQuery().copy(limit = 3))

		assertEquals(3, result.range.actualCount)
		assertEquals(3, result.range.requestedLimit)
		assertEquals(result.points.first().timestamp, result.range.from)
		assertEquals(result.points.last().timestamp, result.range.to)
	}

	private fun serviceWithSource(source: MarketBarSource): MarketIndicatorsQueryService {
		val resolver = mockk<MarketBarSourceResolver>()
		every { resolver.resolve(any()) } returns source
		return MarketIndicatorsQueryService(resolver, BollingerBandsCalculator(), RsiCalculator(), MacdCalculator())
	}

	private fun baseQuery() = MarketIndicatorsQuery(
		symbol = "005930",
		interval = "1m",
		limit = 30,
		from = null,
		to = null,
		indicators = "bb,rsi",
		bbPeriod = 20,
		bbStdDev = BigDecimal("2.0"),
		rsiPeriod = 14,
		macdFast = 12,
		macdSlow = 26,
		macdSignal = 9,
	)

	private fun sampleBars(size: Int): List<MarketBarRecord> = (0 until size).map {
		MarketBarRecord(
			timestamp = Instant.parse("2026-05-01T00:00:00Z").plusSeconds((it * 60).toLong()),
			close = BigDecimal.valueOf((100 + it).toLong()),
		)
	}
}
