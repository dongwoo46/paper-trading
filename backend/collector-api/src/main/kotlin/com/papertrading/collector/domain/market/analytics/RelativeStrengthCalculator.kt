package com.papertrading.collector.domain.market.analytics

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

data class RelativeStrengthPoint(
	val timestamp: Instant,
	val ratio: BigDecimal?,
	val returnDelta: BigDecimal?,
)

class RelativeStrengthCalculator {
	fun calculate(symbolCloses: List<BigDecimal>, baselineCloses: List<BigDecimal>, timestamps: List<Instant>): List<RelativeStrengthPoint> {
		if (symbolCloses.size != baselineCloses.size || symbolCloses.size != timestamps.size) {
			throw IllegalArgumentException("INVALID_PERIOD_QUERY")
		}
		timestamps.zipWithNext().forEach { (a, b) -> if (a > b) throw IllegalArgumentException("INVALID_PERIOD_QUERY") }
		if (symbolCloses.isEmpty()) return emptyList()
		val symbolFirst = symbolCloses.first()
		val baselineFirst = baselineCloses.first()
		return symbolCloses.indices.map { index ->
			val s = symbolCloses[index]
			val b = baselineCloses[index]
			val ratio = if (b.compareTo(BigDecimal.ZERO) == 0) null else s.divide(b, 8, RoundingMode.HALF_UP)
			val symbolRet = if (symbolFirst.compareTo(BigDecimal.ZERO) == 0) null else s.subtract(symbolFirst).divide(symbolFirst, 8, RoundingMode.HALF_UP)
			val baseRet = if (baselineFirst.compareTo(BigDecimal.ZERO) == 0) null else b.subtract(baselineFirst).divide(baselineFirst, 8, RoundingMode.HALF_UP)
			val delta = if (symbolRet == null || baseRet == null) null else symbolRet.subtract(baseRet)
			RelativeStrengthPoint(
				timestamp = timestamps[index],
				ratio = ratio,
				returnDelta = delta,
			)
		}
	}
}
