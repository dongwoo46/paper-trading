package com.papertrading.collector.domain.marketfeature

import java.math.BigDecimal
import java.time.Instant

data class MinuteBarState(
    val minute: String,
    val open: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val close: BigDecimal,
    val volume: BigDecimal,
    val tradeValue: BigDecimal,
    val buyVolume: BigDecimal,
    val sellVolume: BigDecimal,
    val tickCount: Int,
    val startedAt: Instant,
    val updatedAt: Instant,
)

