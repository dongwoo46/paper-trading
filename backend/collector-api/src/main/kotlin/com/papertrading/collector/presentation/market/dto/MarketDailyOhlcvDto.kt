package com.papertrading.collector.presentation.market.dto

import com.papertrading.collector.domain.market.MarketDailyOhlcv
import com.papertrading.collector.infra.market.persistence.MarketDailySymbolSummaryProjection
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class MarketDailySymbolSummaryResponse(
	val symbol: String,
	val market: String,
	val latestTradeDate: LocalDate,
	val totalBars: Long,
)

data class MarketDailyBarResponse(
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

fun MarketDailySymbolSummaryProjection.toResponse(): MarketDailySymbolSummaryResponse {
	return MarketDailySymbolSummaryResponse(
		symbol = getSymbol(),
		market = getMarket(),
		latestTradeDate = getLatestTradeDate(),
		totalBars = getTotalBars(),
	)
}

fun MarketDailyOhlcv.toResponse(): MarketDailyBarResponse {
	return MarketDailyBarResponse(
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

