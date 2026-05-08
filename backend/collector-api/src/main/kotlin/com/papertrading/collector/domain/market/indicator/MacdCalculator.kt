package com.papertrading.collector.domain.market.indicator

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

@Component
class MacdCalculator {
	fun calculate(closes: List<BigDecimal>, params: MacdParams): List<MacdValue?> {
		if (params.fastPeriod <= 0 || params.slowPeriod <= 0 || params.signalPeriod <= 0 || params.fastPeriod >= params.slowPeriod) {
			throw IllegalArgumentException("INVALID_INDICATOR_PARAM: macd")
		}
		if (closes.isEmpty()) return emptyList()
		val result = MutableList<MacdValue?>(closes.size) { null }
		val mc = MathContext(16, RoundingMode.HALF_UP)

		val fastEma = ema(closes, params.fastPeriod, mc)
		val slowEma = ema(closes, params.slowPeriod, mc)
		val macdLine = closes.indices.map { i ->
			if (fastEma[i] == null || slowEma[i] == null) null else fastEma[i]!!.subtract(slowEma[i]!!, mc)
		}

		val firstMacd = macdLine.indexOfFirst { it != null }
		if (firstMacd < 0) return result
		val signalEma = emaNullable(macdLine, params.signalPeriod, mc)
		for (i in closes.indices) {
			val macd = macdLine[i]
			val signal = signalEma[i]
			if (macd != null && signal != null) {
				result[i] = MacdValue(
					macd = macd,
					signal = signal,
					histogram = macd.subtract(signal, mc),
				)
			}
		}
		return result
	}

	private fun ema(values: List<BigDecimal>, period: Int, mc: MathContext): List<BigDecimal?> {
		val result = MutableList<BigDecimal?>(values.size) { null }
		if (values.size < period) return result
		val multiplier = BigDecimal("2").divide(BigDecimal.valueOf((period + 1).toLong()), mc)
		var prev = values.subList(0, period).reduce(BigDecimal::add).divide(BigDecimal.valueOf(period.toLong()), mc)
		result[period - 1] = prev
		for (i in period until values.size) {
			prev = values[i].subtract(prev, mc).multiply(multiplier, mc).add(prev, mc)
			result[i] = prev
		}
		return result
	}

	private fun emaNullable(values: List<BigDecimal?>, period: Int, mc: MathContext): List<BigDecimal?> {
		val result = MutableList<BigDecimal?>(values.size) { null }
		val compact = values.withIndex().filter { it.value != null }
		if (compact.size < period) return result
		val multiplier = BigDecimal("2").divide(BigDecimal.valueOf((period + 1).toLong()), mc)
		var prev = compact.take(period).map { it.value!! }.reduce(BigDecimal::add)
			.divide(BigDecimal.valueOf(period.toLong()), mc)
		result[compact[period - 1].index] = prev
		for (i in period until compact.size) {
			prev = compact[i].value!!.subtract(prev, mc).multiply(multiplier, mc).add(prev, mc)
			result[compact[i].index] = prev
		}
		return result
	}
}

