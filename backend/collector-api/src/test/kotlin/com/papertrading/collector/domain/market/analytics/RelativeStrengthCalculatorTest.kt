package com.papertrading.collector.domain.market.analytics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class RelativeStrengthCalculatorTest {
	@Test
	fun `ratio and returnDelta 계산`() {
		val calculator = RelativeStrengthCalculator()
		val timestamps = listOf(
			Instant.parse("2026-05-01T00:00:00Z"),
			Instant.parse("2026-05-01T00:01:00Z"),
		)
		val result = calculator.calculate(
			symbolCloses = listOf(BigDecimal("100"), BigDecimal("110")),
			baselineCloses = listOf(BigDecimal("200"), BigDecimal("210")),
			timestamps = timestamps,
		)

		assertEquals(2, result.size)
		assertEquals("0.50000000", result[0].ratio.toString())
		assertEquals("0E-8", result[0].returnDelta.toString())
		assertEquals("0.52380952", result[1].ratio.toString())
		assertEquals("0.05000000", result[1].returnDelta.toString())
	}
}
