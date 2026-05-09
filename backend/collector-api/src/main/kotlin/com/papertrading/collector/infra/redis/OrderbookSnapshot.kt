package com.papertrading.collector.infra.redis

import java.math.BigDecimal
import java.time.Instant

data class OrderbookSnapshot(
    val bestBid: BigDecimal?,
    val bestAsk: BigDecimal?,
    val spread: BigDecimal?,
    val bidDepthTopN: BigDecimal?,
    val askDepthTopN: BigDecimal?,
    val depthImbalance: BigDecimal?,
    val timestamp: Instant?,
)
