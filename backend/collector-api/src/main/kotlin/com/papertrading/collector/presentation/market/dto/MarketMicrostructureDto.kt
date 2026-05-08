package com.papertrading.collector.presentation.market.dto

import com.papertrading.collector.application.market.service.MarketMicrostructureResult

data class MarketMicrostructureResponse(
	val symbol: String,
	val interval: String,
	val session: String,
	val timezone: String,
	val range: MarketMicrostructureRangeResponse,
	val microstructure: MarketMicrostructureDataResponse,
	val relativeStrength: RelativeStrengthResponse,
	val meta: MarketMicrostructureMetaResponse,
)

data class MarketMicrostructureRangeResponse(
	val from: String,
	val to: String,
	val requestedLimit: Int?,
	val actualCount: Int,
)

data class MarketMicrostructureDataResponse(
	val bestBid: String?,
	val bestAsk: String?,
	val spread: String?,
	val bidAskImbalance: String?,
	val depth: DepthSummaryResponse,
	val flow: FlowSummaryResponse,
)

data class DepthSummaryResponse(
	val bidDepthTopN: String?,
	val askDepthTopN: String?,
	val depthImbalance: String?,
)

data class FlowSummaryResponse(
	val buyVolume: String?,
	val sellVolume: String?,
	val tradeIntensity: String?,
	val vwap: String?,
	val rvol: String?,
)

data class RelativeStrengthResponse(
	val baseline: RelativeStrengthBaselineResponse,
	val series: List<RelativeStrengthSeriesPointResponse>,
)

data class RelativeStrengthBaselineResponse(
	val benchmark: String?,
	val sector: String?,
)

data class RelativeStrengthSeriesPointResponse(
	val timestamp: String,
	val ratio: String?,
	val returnDelta: String?,
)

data class MarketMicrostructureMetaResponse(
	val missingPolicy: String,
	val warnings: List<String>,
)

fun MarketMicrostructureResult.toResponse(): MarketMicrostructureResponse = MarketMicrostructureResponse(
	symbol = symbol,
	interval = interval,
	session = session,
	timezone = timezone,
	range = MarketMicrostructureRangeResponse(
		from = range.from.toString(),
		to = range.to.toString(),
		requestedLimit = range.requestedLimit,
		actualCount = range.actualCount,
	),
	microstructure = MarketMicrostructureDataResponse(
		bestBid = microstructure.bestBid?.toPlainString(),
		bestAsk = microstructure.bestAsk?.toPlainString(),
		spread = microstructure.spread?.toPlainString(),
		bidAskImbalance = microstructure.bidAskImbalance?.toPlainString(),
		depth = DepthSummaryResponse(
			bidDepthTopN = microstructure.bidDepthTopN?.toPlainString(),
			askDepthTopN = microstructure.askDepthTopN?.toPlainString(),
			depthImbalance = microstructure.depthImbalance?.toPlainString(),
		),
		flow = FlowSummaryResponse(
			buyVolume = microstructure.buyVolume?.toPlainString(),
			sellVolume = microstructure.sellVolume?.toPlainString(),
			tradeIntensity = microstructure.tradeIntensity?.toPlainString(),
			vwap = microstructure.vwap?.toPlainString(),
			rvol = microstructure.rvol?.toPlainString(),
		),
	),
	relativeStrength = RelativeStrengthResponse(
		baseline = RelativeStrengthBaselineResponse(
			benchmark = relativeStrengthBaseline.benchmark,
			sector = relativeStrengthBaseline.sector,
		),
		series = relativeStrengthSeries.map {
			RelativeStrengthSeriesPointResponse(
				timestamp = it.timestamp.toString(),
				ratio = it.ratio?.toPlainString(),
				returnDelta = it.returnDelta?.toPlainString(),
			)
		},
	),
	meta = MarketMicrostructureMetaResponse(
		missingPolicy = "null_field_when_unavailable",
		warnings = warnings,
	),
)
