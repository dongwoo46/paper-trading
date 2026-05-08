package com.papertrading.collector.presentation.market.dto

import com.papertrading.collector.application.market.service.MarketIndicatorsResult

data class MarketIndicatorsResponse(
	val symbol: String,
	val interval: String,
	val range: MarketIndicatorsRangeResponse,
	val series: List<MarketIndicatorPointResponse>,
	val meta: MarketIndicatorsMetaResponse,
)

data class MarketIndicatorsRangeResponse(
	val from: String,
	val to: String,
	val requestedLimit: Int?,
	val actualCount: Int,
)

data class MarketIndicatorsMetaResponse(
	val missingPolicy: String,
	val warnings: List<String>,
)

data class MarketIndicatorPointResponse(
	val timestamp: String,
	val close: String,
	val bb: BoundedBandResponse?,
	val rsi: RsiResponse?,
	val macd: MacdResponse?,
)

data class BoundedBandResponse(
	val middle: String,
	val upper: String,
	val lower: String,
)

data class RsiResponse(
	val value: String,
)

data class MacdResponse(
	val macd: String,
	val signal: String,
	val histogram: String,
)

fun MarketIndicatorsResult.toResponse(): MarketIndicatorsResponse = MarketIndicatorsResponse(
	symbol = symbol,
	interval = interval,
	range = MarketIndicatorsRangeResponse(
		from = range.from.toString(),
		to = range.to.toString(),
		requestedLimit = range.requestedLimit,
		actualCount = range.actualCount,
	),
	series = points.map {
		MarketIndicatorPointResponse(
			timestamp = it.timestamp.toString(),
			close = it.close.toPlainString(),
			bb = it.bb?.let { bb -> BoundedBandResponse(bb.middle.toPlainString(), bb.upper.toPlainString(), bb.lower.toPlainString()) },
			rsi = it.rsi?.let { rsi -> RsiResponse(rsi.value.toPlainString()) },
			macd = it.macd?.let { m -> MacdResponse(m.macd.toPlainString(), m.signal.toPlainString(), m.histogram.toPlainString()) },
		)
	},
	meta = MarketIndicatorsMetaResponse(
		missingPolicy = meta.missingPolicy,
		warnings = meta.warnings,
	),
)
