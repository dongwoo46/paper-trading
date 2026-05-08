package com.papertrading.collector.presentation.market.dto

import com.papertrading.collector.domain.entity.market.MarketWeeklyOhlcv
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class MarketWeeklyBarResponse(
	val source: String,
	val symbol: String,
	val market: String,
	val tradeDate: LocalDate,
	val openPrice: BigDecimal,
	val highPrice: BigDecimal,
	val lowPrice: BigDecimal,
	val closePrice: BigDecimal,
	val volume: BigDecimal,
	val adjClosePrice: BigDecimal?,
	val provider: String,
	val interval: String,
	val isAdjusted: Boolean,
	val collectedAt: LocalDateTime,
)

fun MarketWeeklyOhlcv.toResponse(): MarketWeeklyBarResponse {
	return MarketWeeklyBarResponse(
		source = source,
		symbol = symbol,
		market = market,
		tradeDate = tradeDate,
		openPrice = openPrice,
		highPrice = highPrice,
		lowPrice = lowPrice,
		closePrice = closePrice,
		volume = volume,
		adjClosePrice = adjClosePrice,
		provider = provider,
		interval = interval,
		isAdjusted = isAdjusted,
		collectedAt = collectedAt,
	)
}
