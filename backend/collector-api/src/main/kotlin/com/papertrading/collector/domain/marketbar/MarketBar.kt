package com.papertrading.collector.domain.marketbar

import java.math.BigDecimal
import java.time.Instant

data class MarketBar(
    val symbol: String,
    val interval: String,
    val startedAt: Instant,
    val open: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val close: BigDecimal,
    val volume: BigDecimal,
    val tradeValue: BigDecimal,
    val vwap: BigDecimal,
    val tickCount: Int,
)
