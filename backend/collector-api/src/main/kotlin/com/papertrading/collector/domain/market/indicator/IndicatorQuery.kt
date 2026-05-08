package com.papertrading.collector.domain.market.indicator

import java.time.Instant

enum class Interval(val value: String) {
	ONE_MINUTE("1m"),
	FIVE_MINUTES("5m"),
	TEN_MINUTES("10m"),
	ONE_DAY("1d"),
	ONE_WEEK("1w"),
	;

	companion object {
		fun from(value: String): Interval = entries.find { it.value == value }
			?: throw IllegalArgumentException("INVALID_INTERVAL: $value")
	}
}

enum class IndicatorType {
	BB,
	RSI,
	MACD,
	;

	companion object {
		fun parse(csv: String): Set<IndicatorType> {
			val parsed = csv.split(",")
				.map { it.trim().lowercase() }
				.filter { it.isNotBlank() }
				.map {
					when (it) {
						"bb" -> BB
						"rsi" -> RSI
						"macd" -> MACD
						else -> throw IllegalArgumentException("INVALID_INDICATOR_PARAM: $it")
					}
				}
				.toSet()
			if (parsed.isEmpty()) throw IllegalArgumentException("INVALID_INDICATOR_PARAM: empty indicators")
			return parsed
		}
	}
}

data class QueryPeriod(
	val limit: Int? = null,
	val from: Instant? = null,
	val to: Instant? = null,
)

data class IndicatorQuery(
	val symbol: String,
	val interval: Interval,
	val period: QueryPeriod,
	val indicators: Set<IndicatorType>,
	val bbParams: BollingerBandParams,
	val rsiParams: RsiParams,
	val macdParams: MacdParams,
)

