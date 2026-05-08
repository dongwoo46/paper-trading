package com.papertrading.collector.domain.market.analytics

import java.time.Instant

enum class QueryInterval(val value: String) {
	ONE_MINUTE("1m"),
	FIVE_MINUTES("5m"),
	TEN_MINUTES("10m"),
	ONE_DAY("1d"),
	ONE_WEEK("1w"),
	;

	companion object {
		fun from(value: String): QueryInterval =
			entries.find { it.value == value.lowercase() } ?: throw IllegalArgumentException("INVALID_INTERVAL")
	}
}

enum class MarketSession(val value: String) {
	REGULAR("regular"),
	PRE("pre"),
	AFTER("after"),
	;

	companion object {
		fun from(value: String?): MarketSession {
			if (value.isNullOrBlank()) return REGULAR
			return entries.find { it.value == value.lowercase() } ?: throw IllegalArgumentException("INVALID_SESSION")
		}
	}
}

data class RangePolicy(
	val limit: Int?,
	val from: Instant?,
	val to: Instant?,
) {
	init {
		val hasLimit = limit != null
		val hasRange = from != null || to != null
		if (hasLimit == hasRange) throw IllegalArgumentException("INVALID_PERIOD_QUERY")
		if ((from != null) xor (to != null)) throw IllegalArgumentException("INVALID_PERIOD_QUERY")
		if (limit != null && limit !in 1..2000) throw IllegalArgumentException("INVALID_PERIOD_QUERY")
		if (from != null && to != null && from > to) throw IllegalArgumentException("INVALID_PERIOD_QUERY")
	}
}

data class MarketAnalyticsQuery(
	val symbol: String,
	val interval: QueryInterval,
	val session: MarketSession,
	val range: RangePolicy,
	val benchmark: String?,
	val sector: String?,
)
