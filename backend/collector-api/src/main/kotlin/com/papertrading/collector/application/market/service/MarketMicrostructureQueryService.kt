package com.papertrading.collector.application.market.service

import com.papertrading.collector.application.marketfeature.port.MarketFeatureStore
import com.papertrading.collector.domain.market.analytics.MarketAnalyticsQuery
import com.papertrading.collector.domain.market.analytics.MarketMicrostructureSnapshot
import com.papertrading.collector.domain.market.analytics.MarketSession
import com.papertrading.collector.domain.market.analytics.QueryInterval
import com.papertrading.collector.domain.market.analytics.RangePolicy
import com.papertrading.collector.domain.market.analytics.RelativeStrengthCalculator
import com.papertrading.collector.domain.market.analytics.RelativeStrengthPoint
import com.papertrading.collector.domain.market.indicator.Interval
import com.papertrading.collector.domain.marketfeature.FeatureWindow
import com.papertrading.collector.infra.redis.OrderbookRedisStore
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
	private val marketBarSourceResolver: MarketBarSourceResolver,
	private val marketFeatureStore: MarketFeatureStore,
	private val orderbookRedisStore: OrderbookRedisStore,
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

		val indicatorInterval = interval.toIndicatorInterval()
		val source = marketBarSourceResolver.resolve(indicatorInterval)
		val barRequest = toBarRequest(query, normalized)

		val bars = source.load(query.symbol, indicatorInterval, barRequest)
		if (bars.isEmpty()) throw SymbolNotFoundOrNoDataException()
		val baseSymbol = query.benchmark ?: defaultBenchmark(query.symbol)
		val baselineBars = source.load(baseSymbol, indicatorInterval, barRequest)
		if (baselineBars.size < bars.size) throw InsufficientDataForRsException()

		val rsSeries = relativeStrengthCalculator.calculate(
			symbolCloses = bars.map { it.close },
			baselineCloses = baselineBars.take(bars.size).map { it.close },
			timestamps = bars.map { it.timestamp },
		)

		return MarketMicrostructureResult(
			symbol = query.symbol,
			interval = query.interval.value,
			session = query.session.value,
			timezone = if (isKrSymbol(query.symbol)) "Asia/Seoul" else "America/New_York",
			range = MarketMicrostructureRange(
				from = bars.first().timestamp,
				to = bars.last().timestamp,
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
		val featureSnapshot = marketFeatureStore.loadSnapshot(symbol, window)
		val orderbookSnapshot = orderbookRedisStore.load(symbol)

		if (featureSnapshot == null) {
			return MarketMicrostructureSnapshot(
				bestBid = orderbookSnapshot?.bestBid,
				bestAsk = orderbookSnapshot?.bestAsk,
				spread = orderbookSnapshot?.spread,
				bidAskImbalance = null,
				bidDepthTopN = orderbookSnapshot?.bidDepthTopN,
				askDepthTopN = orderbookSnapshot?.askDepthTopN,
				depthImbalance = orderbookSnapshot?.depthImbalance,
				buyVolume = null,
				sellVolume = null,
				tradeIntensity = null,
				vwap = null,
				rvol = null,
				timestamp = orderbookSnapshot?.timestamp,
			)
		}
		return MarketMicrostructureSnapshot(
			bestBid = orderbookSnapshot?.bestBid,
			bestAsk = orderbookSnapshot?.bestAsk,
			spread = orderbookSnapshot?.spread,
			bidAskImbalance = featureSnapshot.tradeImbalance,
			bidDepthTopN = orderbookSnapshot?.bidDepthTopN,
			askDepthTopN = orderbookSnapshot?.askDepthTopN,
			depthImbalance = orderbookSnapshot?.depthImbalance,
			buyVolume = featureSnapshot.buyVolume,
			sellVolume = featureSnapshot.sellVolume,
			tradeIntensity = featureSnapshot.tradeImbalance,
			vwap = featureSnapshot.vwap,
			rvol = featureSnapshot.volume,
			timestamp = featureSnapshot.updatedAt,
		)
	}

	private fun normalize(request: MarketMicrostructureQuery): MarketMicrostructureQuery {
		val hasLimit = request.limit != null
		val hasRange = request.from != null || request.to != null
		return if (!hasLimit && !hasRange) request.copy(limit = 200) else request
	}

	private fun toBarRequest(query: MarketAnalyticsQuery, normalized: MarketMicrostructureQuery): MarketIndicatorsQuery =
		MarketIndicatorsQuery(
			symbol = query.symbol,
			interval = query.interval.value,
			limit = query.range.limit,
			from = query.range.from,
			to = query.range.to,
			indicators = "rsi",
			bbPeriod = null,
			bbStdDev = null,
			rsiPeriod = null,
			macdFast = null,
			macdSlow = null,
			macdSignal = null,
		)

	private fun defaultBenchmark(symbol: String): String = if (isKrSymbol(symbol)) "KOSPI200" else "SPY"

	private fun isKrSymbol(symbol: String): Boolean = symbol.all { it.isDigit() }
}

private fun QueryInterval.toIndicatorInterval(): Interval = when (this) {
	QueryInterval.ONE_MINUTE -> Interval.ONE_MINUTE
	QueryInterval.FIVE_MINUTES -> Interval.FIVE_MINUTES
	QueryInterval.TEN_MINUTES -> Interval.TEN_MINUTES
	QueryInterval.ONE_DAY -> Interval.ONE_DAY
	QueryInterval.ONE_WEEK -> Interval.ONE_WEEK
}