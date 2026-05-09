package com.papertrading.collector.application.market.service

import com.papertrading.collector.application.marketfeature.port.MarketFeatureStore
import com.papertrading.collector.domain.market.indicator.Interval
import com.papertrading.collector.domain.marketfeature.FeatureSnapshot
import com.papertrading.collector.domain.marketfeature.FeatureWindow
import com.papertrading.collector.infra.redis.OrderbookRedisStore
import com.papertrading.collector.infra.redis.OrderbookSnapshot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class MarketMicrostructureQueryServiceTest {
	@Test
	fun `INVALID_SESSION 검증`() {
		val service = serviceWithBars()
		assertThrows(IllegalArgumentException::class.java) {
			service.query(baseQuery().copy(session = "overnight"))
		}
	}

	@Test
	fun `period 정책 위반(limit와 from-to 동시) 검증`() {
		val service = serviceWithBars()
		assertThrows(IllegalArgumentException::class.java) {
			service.query(
				baseQuery().copy(
					limit = 10,
					from = Instant.parse("2026-05-01T00:00:00Z"),
					to = Instant.parse("2026-05-01T00:10:00Z"),
				),
			)
		}
	}

	@Test
	fun `microstructure snapshot 누락 시 null 필드 반환`() {
		val sourceResolver = mockk<MarketBarSourceResolver>()
		val intradaySource = mockk<MarketBarSource>()
		val featureStore = mockk<MarketFeatureStore>()
		val orderbookStore = mockk<OrderbookRedisStore>()
		every { sourceResolver.resolve(Interval.ONE_MINUTE) } returns intradaySource
		every { intradaySource.load("005930", Interval.ONE_MINUTE, any()) } returns sampleBarRecords("005930", 3)
		every { intradaySource.load("KOSPI200", Interval.ONE_MINUTE, any()) } returns sampleBarRecords("KOSPI200", 3)
		every { featureStore.loadSnapshot("005930", FeatureWindow.M1) } returns null
		every { orderbookStore.load("005930") } returns null
		val service = MarketMicrostructureQueryService(sourceResolver, featureStore, orderbookStore)

		val result = service.query(baseQuery())

		assertNull(result.microstructure.buyVolume)
		assertEquals(3, result.relativeStrengthSeries.size)
	}

	@Test
	fun `baseline bar 부족 시 422 예외`() {
		val sourceResolver = mockk<MarketBarSourceResolver>()
		val intradaySource = mockk<MarketBarSource>()
		val featureStore = mockk<MarketFeatureStore>()
		val orderbookStore = mockk<OrderbookRedisStore>()
		every { sourceResolver.resolve(Interval.ONE_MINUTE) } returns intradaySource
		every { intradaySource.load("005930", Interval.ONE_MINUTE, any()) } returns sampleBarRecords("005930", 3)
		every { intradaySource.load("KOSPI200", Interval.ONE_MINUTE, any()) } returns sampleBarRecords("KOSPI200", 2)
		every { featureStore.loadSnapshot(any(), any()) } returns sampleSnapshot()
		every { orderbookStore.load(any()) } returns null
		val service = MarketMicrostructureQueryService(sourceResolver, featureStore, orderbookStore)

		assertThrows(InsufficientDataForRsException::class.java) {
			service.query(baseQuery())
		}
	}

	@Test
	fun `1d 요청 시 MarketBarSourceResolver를 통해 Postgres 소스 사용됨`() {
		val sourceResolver = mockk<MarketBarSourceResolver>()
		val postgresSource = mockk<MarketBarSource>()
		val featureStore = mockk<MarketFeatureStore>()
		val orderbookStore = mockk<OrderbookRedisStore>()
		every { sourceResolver.resolve(Interval.ONE_DAY) } returns postgresSource
		every { postgresSource.load("005930", Interval.ONE_DAY, any()) } returns sampleBarRecords("005930", 3)
		every { postgresSource.load("KOSPI200", Interval.ONE_DAY, any()) } returns sampleBarRecords("KOSPI200", 3)
		every { featureStore.loadSnapshot(any(), any()) } returns sampleSnapshot()
		val service = MarketMicrostructureQueryService(sourceResolver, featureStore, orderbookStore)

		service.query(baseQuery().copy(interval = "1d"))

		verify(exactly = 1) { sourceResolver.resolve(Interval.ONE_DAY) }
		verify(exactly = 2) { postgresSource.load(any(), Interval.ONE_DAY, any()) }
	}

	@Test
	fun `1w 요청 시 MarketBarSourceResolver를 통해 Postgres 소스 사용됨`() {
		val sourceResolver = mockk<MarketBarSourceResolver>()
		val postgresSource = mockk<MarketBarSource>()
		val featureStore = mockk<MarketFeatureStore>()
		val orderbookStore = mockk<OrderbookRedisStore>()
		every { sourceResolver.resolve(Interval.ONE_WEEK) } returns postgresSource
		every { postgresSource.load("005930", Interval.ONE_WEEK, any()) } returns sampleBarRecords("005930", 3)
		every { postgresSource.load("KOSPI200", Interval.ONE_WEEK, any()) } returns sampleBarRecords("KOSPI200", 3)
		every { featureStore.loadSnapshot(any(), any()) } returns sampleSnapshot()
		val service = MarketMicrostructureQueryService(sourceResolver, featureStore, orderbookStore)

		service.query(baseQuery().copy(interval = "1w"))

		verify(exactly = 1) { sourceResolver.resolve(Interval.ONE_WEEK) }
		verify(exactly = 2) { postgresSource.load(any(), Interval.ONE_WEEK, any()) }
	}

	@Test
	fun `intraday 요청 시 orderbook null이어도 나머지 필드 정상 반환`() {
		val sourceResolver = mockk<MarketBarSourceResolver>()
		val intradaySource = mockk<MarketBarSource>()
		val featureStore = mockk<MarketFeatureStore>()
		val orderbookStore = mockk<OrderbookRedisStore>()
		every { sourceResolver.resolve(Interval.ONE_MINUTE) } returns intradaySource
		every { intradaySource.load("005930", Interval.ONE_MINUTE, any()) } returns sampleBarRecords("005930", 3)
		every { intradaySource.load("KOSPI200", Interval.ONE_MINUTE, any()) } returns sampleBarRecords("KOSPI200", 3)
		every { featureStore.loadSnapshot("005930", FeatureWindow.M1) } returns sampleSnapshot()
		every { orderbookStore.load("005930") } returns null
		val service = MarketMicrostructureQueryService(sourceResolver, featureStore, orderbookStore)

		val result = service.query(baseQuery())

		assertNull(result.microstructure.bestBid)
		assertNotNull(result.microstructure.buyVolume)
	}

	@Test
	fun `intraday 요청 시 orderbook 있을 때 bestBid bestAsk spread depth 매핑됨`() {
		val sourceResolver = mockk<MarketBarSourceResolver>()
		val intradaySource = mockk<MarketBarSource>()
		val featureStore = mockk<MarketFeatureStore>()
		val orderbookStore = mockk<OrderbookRedisStore>()
		val snapshot = OrderbookSnapshot(
			bestBid = BigDecimal("75000"),
			bestAsk = BigDecimal("75100"),
			spread = BigDecimal("100"),
			bidDepthTopN = BigDecimal("5000"),
			askDepthTopN = BigDecimal("4000"),
			depthImbalance = BigDecimal("0.11111111"),
			timestamp = Instant.parse("2026-05-01T00:01:00Z"),
		)
		every { sourceResolver.resolve(Interval.ONE_MINUTE) } returns intradaySource
		every { intradaySource.load("005930", Interval.ONE_MINUTE, any()) } returns sampleBarRecords("005930", 3)
		every { intradaySource.load("KOSPI200", Interval.ONE_MINUTE, any()) } returns sampleBarRecords("KOSPI200", 3)
		every { featureStore.loadSnapshot("005930", FeatureWindow.M1) } returns sampleSnapshot()
		every { orderbookStore.load("005930") } returns snapshot
		val service = MarketMicrostructureQueryService(sourceResolver, featureStore, orderbookStore)

		val result = service.query(baseQuery())

		assertEquals(BigDecimal("75000"), result.microstructure.bestBid)
		assertEquals(BigDecimal("75100"), result.microstructure.bestAsk)
		assertEquals(BigDecimal("100"), result.microstructure.spread)
		assertEquals(BigDecimal("5000"), result.microstructure.bidDepthTopN)
		assertEquals(BigDecimal("4000"), result.microstructure.askDepthTopN)
		assertEquals(BigDecimal("0.11111111"), result.microstructure.depthImbalance)
	}

	@Test
	fun `1d 요청 시 microstructure depth 전체 null`() {
		val sourceResolver = mockk<MarketBarSourceResolver>()
		val postgresSource = mockk<MarketBarSource>()
		val featureStore = mockk<MarketFeatureStore>()
		val orderbookStore = mockk<OrderbookRedisStore>()
		every { sourceResolver.resolve(Interval.ONE_DAY) } returns postgresSource
		every { postgresSource.load("005930", Interval.ONE_DAY, any()) } returns sampleBarRecords("005930", 3)
		every { postgresSource.load("KOSPI200", Interval.ONE_DAY, any()) } returns sampleBarRecords("KOSPI200", 3)
		every { featureStore.loadSnapshot(any(), any()) } returns sampleSnapshot()
		val service = MarketMicrostructureQueryService(sourceResolver, featureStore, orderbookStore)

		val result = service.query(baseQuery().copy(interval = "1d"))

		assertNull(result.microstructure.bestBid)
		assertNull(result.microstructure.bestAsk)
		assertNull(result.microstructure.spread)
		assertNull(result.microstructure.bidDepthTopN)
		assertNull(result.microstructure.askDepthTopN)
		assertNull(result.microstructure.depthImbalance)
	}

	@Test
	fun `1w 요청 시 microstructure depth 전체 null`() {
		val sourceResolver = mockk<MarketBarSourceResolver>()
		val postgresSource = mockk<MarketBarSource>()
		val featureStore = mockk<MarketFeatureStore>()
		val orderbookStore = mockk<OrderbookRedisStore>()
		every { sourceResolver.resolve(Interval.ONE_WEEK) } returns postgresSource
		every { postgresSource.load("005930", Interval.ONE_WEEK, any()) } returns sampleBarRecords("005930", 3)
		every { postgresSource.load("KOSPI200", Interval.ONE_WEEK, any()) } returns sampleBarRecords("KOSPI200", 3)
		every { featureStore.loadSnapshot(any(), any()) } returns sampleSnapshot()
		val service = MarketMicrostructureQueryService(sourceResolver, featureStore, orderbookStore)

		val result = service.query(baseQuery().copy(interval = "1w"))

		assertNull(result.microstructure.bestBid)
		assertNull(result.microstructure.bestAsk)
		assertNull(result.microstructure.spread)
		assertNull(result.microstructure.bidDepthTopN)
		assertNull(result.microstructure.askDepthTopN)
		assertNull(result.microstructure.depthImbalance)
	}

	private fun serviceWithBars(): MarketMicrostructureQueryService {
		val sourceResolver = mockk<MarketBarSourceResolver>()
		val intradaySource = mockk<MarketBarSource>()
		val featureStore = mockk<MarketFeatureStore>()
		val orderbookStore = mockk<OrderbookRedisStore>()
		every { sourceResolver.resolve(Interval.ONE_MINUTE) } returns intradaySource
		every { intradaySource.load("005930", Interval.ONE_MINUTE, any()) } returns sampleBarRecords("005930", 3)
		every { intradaySource.load("KOSPI200", Interval.ONE_MINUTE, any()) } returns sampleBarRecords("KOSPI200", 3)
		every { featureStore.loadSnapshot(any(), any()) } returns sampleSnapshot()
		every { orderbookStore.load(any()) } returns null
		return MarketMicrostructureQueryService(sourceResolver, featureStore, orderbookStore)
	}

	private fun baseQuery() = MarketMicrostructureQuery(
		symbol = "005930",
		interval = "1m",
		session = "regular",
		limit = 3,
		from = null,
		to = null,
		benchmark = null,
		sector = null,
	)

	private fun sampleBarRecords(symbol: String, size: Int): List<MarketBarRecord> = (0 until size).map {
		MarketBarRecord(
			timestamp = Instant.parse("2026-05-01T00:00:00Z").plusSeconds((it * 60).toLong()),
			close = BigDecimal.valueOf((100 + it).toLong()),
		)
	}

	private fun sampleSnapshot(): FeatureSnapshot = FeatureSnapshot(
		window = FeatureWindow.M1,
		open = BigDecimal("100"),
		high = BigDecimal("101"),
		low = BigDecimal("99"),
		close = BigDecimal("100"),
		returnRate = BigDecimal("0.01"),
		volume = BigDecimal("1000"),
		tradeValue = BigDecimal("100000"),
		vwap = BigDecimal("100"),
		buyVolume = BigDecimal("600"),
		sellVolume = BigDecimal("400"),
		tradeImbalance = BigDecimal("0.2"),
		tickCount = 10,
		startedAt = Instant.parse("2026-05-01T00:00:00Z"),
		updatedAt = Instant.parse("2026-05-01T00:01:00Z"),
	)
}