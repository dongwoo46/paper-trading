package com.papertrading.collector.domain.market.analytics

import java.math.BigDecimal
import java.time.Instant

data class MarketMicrostructureSnapshot(
	val bestBid: BigDecimal?,
	val bestAsk: BigDecimal?,
	val spread: BigDecimal?,
	val bidAskImbalance: BigDecimal?,
	val bidDepthTopN: BigDecimal?,
	val askDepthTopN: BigDecimal?,
	val depthImbalance: BigDecimal?,
	val buyVolume: BigDecimal?,
	val sellVolume: BigDecimal?,
	val tradeIntensity: BigDecimal?,
	val vwap: BigDecimal?,
	val rvol: BigDecimal?,
	val timestamp: Instant?,
)
