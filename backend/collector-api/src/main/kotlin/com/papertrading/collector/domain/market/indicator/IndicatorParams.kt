package com.papertrading.collector.domain.market.indicator

import java.math.BigDecimal

data class BollingerBandParams(
	val period: Int = 20,
	val stdDevMultiplier: BigDecimal = BigDecimal("2.0"),
)

data class RsiParams(
	val period: Int = 14,
)

data class MacdParams(
	val fastPeriod: Int = 12,
	val slowPeriod: Int = 26,
	val signalPeriod: Int = 9,
)

