package com.papertrading.collector.domain.market.indicator

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

@Component
class RsiCalculator {
	fun calculate(closes: List<BigDecimal>, params: RsiParams): List<RsiValue?> {
		if (params.period <= 0) throw IllegalArgumentException("INVALID_INDICATOR_PARAM: rsiPeriod")
		if (closes.isEmpty()) return emptyList()
		val result = MutableList<RsiValue?>(closes.size) { null }
		if (closes.size <= params.period) return result

		val mc = MathContext(16, RoundingMode.HALF_UP)
		var gainSum = BigDecimal.ZERO
		var lossSum = BigDecimal.ZERO
		for (i in 1..params.period) {
			val diff = closes[i].subtract(closes[i - 1], mc)
			if (diff >= BigDecimal.ZERO) gainSum = gainSum.add(diff, mc) else lossSum = lossSum.add(diff.abs(), mc)
		}
		var avgGain = gainSum.divide(BigDecimal.valueOf(params.period.toLong()), mc)
		var avgLoss = lossSum.divide(BigDecimal.valueOf(params.period.toLong()), mc)
		result[params.period] = RsiValue(calculateRsi(avgGain, avgLoss, mc))

		for (i in (params.period + 1) until closes.size) {
			val diff = closes[i].subtract(closes[i - 1], mc)
			val gain = if (diff > BigDecimal.ZERO) diff else BigDecimal.ZERO
			val loss = if (diff < BigDecimal.ZERO) diff.abs() else BigDecimal.ZERO
			avgGain = avgGain.multiply(BigDecimal.valueOf((params.period - 1).toLong()), mc).add(gain, mc)
				.divide(BigDecimal.valueOf(params.period.toLong()), mc)
			avgLoss = avgLoss.multiply(BigDecimal.valueOf((params.period - 1).toLong()), mc).add(loss, mc)
				.divide(BigDecimal.valueOf(params.period.toLong()), mc)
			result[i] = RsiValue(calculateRsi(avgGain, avgLoss, mc))
		}
		return result
	}

	private fun calculateRsi(avgGain: BigDecimal, avgLoss: BigDecimal, mc: MathContext): BigDecimal {
		if (avgLoss.compareTo(BigDecimal.ZERO) == 0) return BigDecimal("100")
		val rs = avgGain.divide(avgLoss, mc)
		return BigDecimal("100").subtract(BigDecimal("100").divide(BigDecimal.ONE.add(rs, mc), mc), mc)
	}
}

