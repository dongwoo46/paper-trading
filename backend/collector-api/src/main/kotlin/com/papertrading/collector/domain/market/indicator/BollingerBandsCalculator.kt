package com.papertrading.collector.domain.market.indicator

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.sqrt

@Component
class BollingerBandsCalculator {
	fun calculate(closes: List<BigDecimal>, params: BollingerBandParams): List<BollingerBandValue?> {
		if (params.period <= 1) throw IllegalArgumentException("INVALID_INDICATOR_PARAM: bbPeriod")
		if (closes.isEmpty()) return emptyList()
		val result = MutableList<BollingerBandValue?>(closes.size) { null }
		val mc = MathContext(16, RoundingMode.HALF_UP)
		for (i in (params.period - 1) until closes.size) {
			val window = closes.subList(i - params.period + 1, i + 1)
			val mean = window.reduce(BigDecimal::add).divide(BigDecimal.valueOf(params.period.toLong()), mc)
			val variance = window.map { it.subtract(mean, mc).pow(2, mc) }
				.reduce(BigDecimal::add)
				.divide(BigDecimal.valueOf(params.period.toLong()), mc)
			val stdDev = BigDecimal.valueOf(sqrt(variance.toDouble()))
			val delta = params.stdDevMultiplier.multiply(stdDev, mc)
			result[i] = BollingerBandValue(
				middle = mean,
				upper = mean.add(delta, mc),
				lower = mean.subtract(delta, mc),
			)
		}
		return result
	}
}

