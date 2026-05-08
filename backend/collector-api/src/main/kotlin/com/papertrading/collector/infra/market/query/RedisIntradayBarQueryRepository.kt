package com.papertrading.collector.infra.market.query

import com.papertrading.collector.application.market.service.MarketBarRecord
import com.papertrading.collector.application.market.service.MarketBarSource
import com.papertrading.collector.application.market.service.MarketIndicatorsQuery
import com.papertrading.collector.domain.market.indicator.Interval
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class RedisIntradayBarQueryRepository(
	private val stringRedisTemplate: StringRedisTemplate,
) : MarketBarSource {
	override fun load(symbol: String, interval: Interval, request: MarketIndicatorsQuery): List<MarketBarRecord> {
		if (interval !in setOf(Interval.ONE_MINUTE, Interval.FIVE_MINUTES, Interval.TEN_MINUTES)) {
			throw IllegalArgumentException("INVALID_INTERVAL")
		}
		val key = "bars:${interval.value}:$symbol"
		val rawValues = if (request.from != null && request.to != null) {
			stringRedisTemplate.opsForZSet().rangeByScore(
				key,
				request.from.toEpochMilli().toDouble(),
				request.to.toEpochMilli().toDouble(),
			) ?: emptySet()
		} else {
			stringRedisTemplate.opsForZSet().reverseRange(key, 0, (request.limit ?: 200).toLong() - 1) ?: emptySet()
		}

		val records = rawValues.mapNotNull { parseRecord(it) }
		val timeFiltered = if (request.from != null && request.to != null) {
			records.filter { it.timestamp >= request.from && it.timestamp <= request.to }
		} else {
			records
		}
		return timeFiltered.sortedBy { it.timestamp }
	}

	private fun parseRecord(raw: String): MarketBarRecord? {
		val parts = raw.split(",")
		if (parts.size < 2) return null
		return runCatching {
			MarketBarRecord(Instant.parse(parts[0]), parts[1].toBigDecimal())
		}.getOrNull()
	}
}
