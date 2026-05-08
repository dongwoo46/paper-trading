package com.papertrading.collector.domain.marketfeature

import java.math.BigDecimal
import java.time.Instant

data class FeatureSnapshot(
    val window: FeatureWindow,
    val open: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val close: BigDecimal,
    val returnRate: BigDecimal,
    val volume: BigDecimal,
    val tradeValue: BigDecimal,
    val vwap: BigDecimal,
    val buyVolume: BigDecimal,
    val sellVolume: BigDecimal,
    val tradeImbalance: BigDecimal,
    val tickCount: Int,
    val startedAt: Instant,
    val updatedAt: Instant,
)

