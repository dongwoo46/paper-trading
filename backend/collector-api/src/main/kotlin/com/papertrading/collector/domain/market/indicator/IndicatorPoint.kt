package com.papertrading.collector.domain.market.indicator

import java.math.BigDecimal
import java.time.Instant

data class BollingerBandValue(
	val middle: BigDecimal,
	val upper: BigDecimal,
	val lower: BigDecimal,
)

data class RsiValue(
	val value: BigDecimal,
)

data class MacdValue(
	val macd: BigDecimal,
	val signal: BigDecimal,
	val histogram: BigDecimal,
)

data class IndicatorPoint(
	val timestamp: Instant,
	val close: BigDecimal,
	val bb: BollingerBandValue?,
	val rsi: RsiValue?,
	val macd: MacdValue?,
)

