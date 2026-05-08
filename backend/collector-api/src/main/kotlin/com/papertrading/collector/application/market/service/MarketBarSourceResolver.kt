package com.papertrading.collector.application.market.service

import com.papertrading.collector.domain.market.indicator.Interval
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

data class MarketBarRecord(
	val timestamp: Instant,
	val close: BigDecimal,
)

interface MarketBarSource {
	fun load(symbol: String, interval: Interval, request: MarketIndicatorsQuery): List<MarketBarRecord>
}

@Component
class MarketBarSourceResolver(
	private val redisIntradayBarQueryRepository: com.papertrading.collector.infra.market.query.RedisIntradayBarQueryRepository,
	private val postgresDailyWeeklyBarQueryRepository: com.papertrading.collector.infra.market.query.PostgresDailyWeeklyBarQueryRepository,
) {
	fun resolve(interval: Interval): MarketBarSource = when (interval) {
		Interval.ONE_MINUTE, Interval.FIVE_MINUTES, Interval.TEN_MINUTES -> redisIntradayBarQueryRepository
		Interval.ONE_DAY, Interval.ONE_WEEK -> postgresDailyWeeklyBarQueryRepository
	}
}

