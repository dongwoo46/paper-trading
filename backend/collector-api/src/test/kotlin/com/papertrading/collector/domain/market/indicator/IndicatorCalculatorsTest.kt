package com.papertrading.collector.domain.market.indicator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class IndicatorCalculatorsTest {
	@Test
	fun `BB warm-up 구간은 null`() {
		val calc = BollingerBandsCalculator()
		val closes = listOf("1", "2", "3", "4", "5").map(::BigDecimal)
		val result = calc.calculate(closes, BollingerBandParams(period = 3, stdDevMultiplier = BigDecimal("2.0")))
		assertNull(result[0])
		assertNull(result[1])
		assertEquals(5, result.size)
	}

	@Test
	fun `RSI warm-up 구간은 null`() {
		val calc = RsiCalculator()
		val closes = listOf("1", "2", "3", "4", "5", "6").map(::BigDecimal)
		val result = calc.calculate(closes, RsiParams(period = 3))
		assertNull(result[0])
		assertNull(result[1])
		assertNull(result[2])
	}

	@Test
	fun `MACD warm-up 구간은 null`() {
		val calc = MacdCalculator()
		val closes = (1..20).map { BigDecimal.valueOf(it.toLong()) }
		val result = calc.calculate(closes, MacdParams(fastPeriod = 3, slowPeriod = 6, signalPeriod = 4))
		assertNull(result[0])
		assertNull(result[1])
		assertEquals(20, result.size)
	}
}

