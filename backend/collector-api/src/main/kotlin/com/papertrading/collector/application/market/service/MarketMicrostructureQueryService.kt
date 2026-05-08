package com.papertrading.collector.application.market.service

import com.papertrading.collector.application.marketbar.port.MarketBarRepository
import com.papertrading.collector.application.marketfeature.port.MarketFeatureStore
import com.papertrading.collector.domain.market.analytics.MarketAnalyticsQuery
import com.papertrading.collector.domain.market.analytics.MarketMicrostructureSnapshot
import com.papertrading.collector.domain.market.analytics.MarketSession
import com.papertrading.collector.domain.market.analytics.QueryInterval
import com.papertrading.collector.domain.market.analytics.RangePolicy
import com.papertrading.collector.domain.market.analytics.RelativeStrengthCalculator
import com.papertrading.collector.domain.market.analytics.RelativeStrengthPoint
import com.papertrading.collector.domain.marketfeature.FeatureWindow
import org.springframework.stereotype.Service
import java.time.Instant

data class MarketMicrostructureQuery(
	val symbol: String,
	val interval: String,
	val session: String?,
	val limit: Int?,
	val from: Instant?,
	val to: Instant?,
	val benchmark: String?,
	val sector: String?,
)

data class MarketMicrostructureRange(
	val from: Instant,
	val to: Instant,
	val requestedLimit: Int?,
	val actualCount: Int,
)

data class RelativeStrengthBaseline(
	val benchmark: String?,
	val sector: String?,
)

data class MarketMicrostructureResult(
	val symbol: String,
	val interval: String,
	val session: String,
	val timezone: String,
	val range: MarketMicrostructureRange,
	val microstructure: MarketMicrostructureSnapshot,
	val relativeStrengthBaseline: RelativeStrengthBaseline,
	val relativeStrengthSeries: List<RelativeStrengthPoint>,
	val warnings: List<String>,
)

@Service
class MarketMicrostructureQueryService(
	private val marketBarRepository: MarketBarRepository,
	private val marketFeatureStore: MarketFeatureStore,
	private val relativeStrengthCalculator: RelativeStrengthCalculator = RelativeStrengthCalculator(),
) {
	fun query(request: MarketMicrostructureQuery): MarketMicrostructureResult {
		if (request.symbol.isBlank()) throw IllegalArgumentException("INVALID_SYMBOL")
		val normalized = normalize(request)
		val interval = QueryInterval.from(normalized.interval)
		val session = MarketSession.from(normalized.session)
		val range = RangePolicy(normalized.limit, normalized.from, normalized.to)
		val query = MarketAnalyticsQuery(
			symbol = normalized.symbol.trim().uppercase(),
			interval = interval,
			session = session,
			range = range,
			benchmark = normalized.benchmark?.trim()?.uppercase(),
			sector = normalized.sector?.trim()?.uppercase(),
		)

		val limit = query.range.limit ?: 200
		val bars = marketBarRepository.findBars(query.symbol, query.interval.value, limit)
		if (bars.isEmpty()) throw SymbolNotFoundOrNoDataException()
		val baseSymbol = query.benchmark ?: defaultBenchmark(query.symbol)
		val baselineBars = marketBarRepository.findBars(baseSymbol, query.interval.value, limit)
		if (baselineBars.size < bars.size) throw InsufficientDataForRsException()

		val rsSeries = relativeStrengthCalculator.calculate(
			symbolCloses = bars.map { it.close },
			baselineCloses = baselineBars.take(bars.size).map { it.close },
			timestamps = bars.map { it.startedAt },
		)

		return MarketMicrostructureResult(
			symbol = query.symbol,
			interval = query.interval.value,
			session = query.session.value,
			timezone = if (isKrSymbol(query.symbol)) "Asia/Seoul" else "America/New_York",
			range = MarketMicrostructureRange(
				from = bars.first().startedAt,
				to = bars.last().startedAt,
				requestedLimit = query.range.limit,
				actualCount = bars.size,
			),
			microstructure = loadSnapshot(query.symbol, query.interval),
			relativeStrengthBaseline = RelativeStrengthBaseline(baseSymbol, query.sector),
			relativeStrengthSeries = rsSeries,
			warnings = emptyList(),
		)
	}

	private fun loadSnapshot(symbol: String, interval: QueryInterval): MarketMicrostructureSnapshot {
		val window = when (interval) {
			QueryInterval.ONE_MINUTE -> FeatureWindow.M1
			QueryInterval.FIVE_MINUTES -> FeatureWindow.M5
			QueryInterval.TEN_MINUTES -> FeatureWindow.M10
			QueryInterval.ONE_DAY, QueryInterval.ONE_WEEK -> null
		}
		if (window == null) {
			return MarketMicrostructureSnapshot(
				bestBid = null,
				bestAsk = null,
				spread = null,
				bidAskImbalance = null,
				bidDepthTopN = null,
				askDepthTopN = null,
				depthImbalance = null,
				buyVolume = null,
				sellVolume = null,
				tradeIntensity = null,
				vwap = null,
				rvol = null,
				timestamp = null,
			)
		}
		val snapshot = marketFeatureStore.loadSnapshot(symbol, window)
		if (snapshot == null) {
			return MarketMicrostructureSnapshot(
				bestBid = null,
				bestAsk = null,
				spread = null,
				bidAskImbalance = null,
				bidDepthTopN = null,
				askDepthTopN = null,
				depthImbalance = null,
				buyVolume = null,
				sellVolume = null,
				tradeIntensity = null,
				vwap = null,
				rvol = null,
				timestamp = null,
			)
		}
		return MarketMicrostructureSnapshot(
			bestBid = null,
			bestAsk = null,
			spread = null,
			bidAskImbalance = snapshot.tradeImbalance,
			bidDepthTopN = null,
			askDepthTopN = null,
			depthImbalance = null,
			buyVolume = snapshot.buyVolume,
			sellVolume = snapshot.sellVolume,
			tradeIntensity = snapshot.tradeImbalance,
			vwap = snapshot.vwap,
			rvol = snapshot.volume,
			timestamp = snapshot.updatedAt,
		)
	}

	private fun normalize(request: MarketMicrostructureQuery): MarketMicrostructureQuery {
		val hasLimit = request.limit != null
		val hasRange = request.from != null || request.to != null
		return if (!hasLimit && !hasRange) request.copy(limit = 200) else request
	}

	private fun defaultBenchmark(symbol: String): String = if (isKrSymbol(symbol)) "KOSPI200" else "SPY"

	private fun isKrSymbol(symbol: String): Boolean = symbol.all { it.isDigit() }
}
