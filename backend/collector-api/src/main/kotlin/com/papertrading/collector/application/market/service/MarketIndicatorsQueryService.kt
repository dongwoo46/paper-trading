package com.papertrading.collector.application.market.service

import com.papertrading.collector.domain.market.indicator.BollingerBandParams
import com.papertrading.collector.domain.market.indicator.BollingerBandsCalculator
import com.papertrading.collector.domain.market.indicator.IndicatorPoint
import com.papertrading.collector.domain.market.indicator.IndicatorQuery
import com.papertrading.collector.domain.market.indicator.IndicatorType
import com.papertrading.collector.domain.market.indicator.Interval
import com.papertrading.collector.domain.market.indicator.MacdCalculator
import com.papertrading.collector.domain.market.indicator.MacdParams
import com.papertrading.collector.domain.market.indicator.QueryPeriod
import com.papertrading.collector.domain.market.indicator.RsiCalculator
import com.papertrading.collector.domain.market.indicator.RsiParams
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant

data class MarketIndicatorsQuery(
	val symbol: String,
	val interval: String,
	val limit: Int?,
	val from: Instant?,
	val to: Instant?,
	val indicators: String,
	val bbPeriod: Int?,
	val bbStdDev: BigDecimal?,
	val rsiPeriod: Int?,
	val macdFast: Int?,
	val macdSlow: Int?,
	val macdSignal: Int?,
)

data class MarketIndicatorsRange(
	val from: Instant,
	val to: Instant,
	val requestedLimit: Int?,
	val actualCount: Int,
)

data class MarketIndicatorsMeta(
	val missingPolicy: String = "null_until_window_ready",
	val warnings: List<String> = emptyList(),
)

data class MarketIndicatorsResult(
	val symbol: String,
	val interval: String,
	val range: MarketIndicatorsRange,
	val points: List<IndicatorPoint>,
	val meta: MarketIndicatorsMeta = MarketIndicatorsMeta(),
)

@Service
class MarketIndicatorsQueryService(
	private val sourceResolver: MarketBarSourceResolver,
	private val bollingerBandsCalculator: BollingerBandsCalculator,
	private val rsiCalculator: RsiCalculator,
	private val macdCalculator: MacdCalculator,
) {
	fun query(request: MarketIndicatorsQuery): MarketIndicatorsResult {
		val normalized = normalize(request)
		validate(normalized)
		val interval = Interval.from(request.interval)
		val indicatorSet = IndicatorType.parse(request.indicators)
		val query = IndicatorQuery(
			symbol = request.symbol.trim().uppercase(),
			interval = interval,
			period = QueryPeriod(limit = normalized.limit, from = normalized.from, to = normalized.to),
			indicators = indicatorSet,
			bbParams = BollingerBandParams(
				period = request.bbPeriod ?: 20,
				stdDevMultiplier = request.bbStdDev ?: BigDecimal("2.0"),
			),
			rsiParams = RsiParams(request.rsiPeriod ?: 14),
			macdParams = MacdParams(
				fastPeriod = request.macdFast ?: 12,
				slowPeriod = request.macdSlow ?: 26,
				signalPeriod = request.macdSignal ?: 9,
			),
		)
		val bars = sourceResolver.resolve(interval).load(query.symbol, interval, normalized)
		if (bars.isEmpty()) throw SymbolNotFoundOrNoBarsException()
		if (normalized.limit != null && bars.size < normalized.limit) {
			throw InsufficientBarsForRequestedRangeException()
		}
		val closes = bars.map { it.close }

		val bb = if (IndicatorType.BB in indicatorSet) bollingerBandsCalculator.calculate(closes, query.bbParams) else List(closes.size) { null }
		val rsi = if (IndicatorType.RSI in indicatorSet) rsiCalculator.calculate(closes, query.rsiParams) else List(closes.size) { null }
		val macd = if (IndicatorType.MACD in indicatorSet) macdCalculator.calculate(closes, query.macdParams) else List(closes.size) { null }

		val points = bars.indices.map { i ->
			IndicatorPoint(
				timestamp = bars[i].timestamp,
				close = bars[i].close,
				bb = bb[i],
				rsi = rsi[i],
				macd = macd[i],
			)
		}
		return MarketIndicatorsResult(
			symbol = query.symbol,
			interval = interval.value,
			range = MarketIndicatorsRange(
				from = points.first().timestamp,
				to = points.last().timestamp,
				requestedLimit = normalized.limit,
				actualCount = points.size,
			),
			points = points,
		)
	}

	private fun normalize(request: MarketIndicatorsQuery): MarketIndicatorsQuery {
		val hasLimit = request.limit != null
		val hasAnyRange = request.from != null || request.to != null
		return if (!hasLimit && !hasAnyRange) request.copy(limit = 200) else request
	}

	private fun validate(request: MarketIndicatorsQuery) {
		if (request.symbol.isBlank()) throw IllegalArgumentException("INVALID_SYMBOL")
		val hasLimit = request.limit != null
		val hasRange = request.from != null && request.to != null
		if (hasLimit == hasRange) throw IllegalArgumentException("INVALID_PERIOD_QUERY")
		if ((request.from != null) xor (request.to != null)) {
			throw IllegalArgumentException("INVALID_PERIOD_QUERY")
		}
		val from = request.from
		val to = request.to
		if (hasRange && from != null && to != null && from > to) {
			throw IllegalArgumentException("INVALID_PERIOD_QUERY")
		}
		if (request.limit != null && request.limit !in 1..2000) throw IllegalArgumentException("INVALID_PERIOD_QUERY")
		if ((request.macdFast ?: 12) >= (request.macdSlow ?: 26)) throw IllegalArgumentException("INVALID_INDICATOR_PARAM")
	}
}
