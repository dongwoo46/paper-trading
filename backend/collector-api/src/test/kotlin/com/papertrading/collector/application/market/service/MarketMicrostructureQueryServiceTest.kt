package com.papertrading.collector.application.market.service

import com.papertrading.collector.application.marketbar.port.MarketBarRepository
import com.papertrading.collector.application.marketfeature.port.MarketFeatureStore
import com.papertrading.collector.domain.marketbar.MarketBar
import com.papertrading.collector.domain.marketfeature.FeatureSnapshot
import com.papertrading.collector.domain.marketfeature.FeatureWindow
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
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
		val barRepo = mockk<MarketBarRepository>()
		val featureStore = mockk<MarketFeatureStore>()
		every { barRepo.findBars("005930", "1m", any()) } returns sampleBars("005930", 3)
		every { barRepo.findBars("KOSPI200", "1m", any()) } returns sampleBars("KOSPI200", 3)
		every { featureStore.loadSnapshot("005930", FeatureWindow.M1) } returns null
		val service = MarketMicrostructureQueryService(barRepo, featureStore)

		val result = service.query(baseQuery())

		assertEquals(null, result.microstructure.buyVolume)
		assertEquals(3, result.relativeStrengthSeries.size)
	}

	@Test
	fun `baseline bar 부족 시 422 예외`() {
		val barRepo = mockk<MarketBarRepository>()
		val featureStore = mockk<MarketFeatureStore>()
		every { barRepo.findBars("005930", "1m", any()) } returns sampleBars("005930", 3)
		every { barRepo.findBars("KOSPI200", "1m", any()) } returns sampleBars("KOSPI200", 2)
		every { featureStore.loadSnapshot(any(), any()) } returns sampleSnapshot()
		val service = MarketMicrostructureQueryService(barRepo, featureStore)

		assertThrows(InsufficientDataForRsException::class.java) {
			service.query(baseQuery())
		}
	}

	private fun serviceWithBars(): MarketMicrostructureQueryService {
		val barRepo = mockk<MarketBarRepository>()
		val featureStore = mockk<MarketFeatureStore>()
		every { barRepo.findBars("005930", "1m", any()) } returns sampleBars("005930", 3)
		every { barRepo.findBars("KOSPI200", "1m", any()) } returns sampleBars("KOSPI200", 3)
		every { featureStore.loadSnapshot(any(), any()) } returns sampleSnapshot()
		return MarketMicrostructureQueryService(barRepo, featureStore)
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

	private fun sampleBars(symbol: String, size: Int): List<MarketBar> = (0 until size).map {
		MarketBar(
			symbol = symbol,
			interval = "1m",
			startedAt = Instant.parse("2026-05-01T00:00:00Z").plusSeconds((it * 60).toLong()),
			open = BigDecimal.valueOf((100 + it).toLong()),
			high = BigDecimal.valueOf((101 + it).toLong()),
			low = BigDecimal.valueOf((99 + it).toLong()),
			close = BigDecimal.valueOf((100 + it).toLong()),
			volume = BigDecimal("1000"),
			tradeValue = BigDecimal("100000"),
			vwap = BigDecimal("100"),
			tickCount = 10,
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
