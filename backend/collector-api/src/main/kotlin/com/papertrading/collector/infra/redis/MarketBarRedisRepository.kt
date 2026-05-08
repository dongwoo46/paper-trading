package com.papertrading.collector.infra.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.collector.application.marketbar.port.MarketBarRepository
import com.papertrading.collector.domain.marketbar.MarketBar
import com.papertrading.collector.domain.marketfeature.MinuteBarState
import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger {}

@Repository
class MarketBarRedisRepository(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : MarketBarRepository {

    override fun findBars(symbol: String, interval: String, limit: Int): List<MarketBar> {
        return when (interval) {
            "1m" -> find1mBars(symbol, limit)
            "5m" -> findAggregatedBars(symbol, interval, limit, intervalMinutes = 5)
            "10m" -> findAggregatedBars(symbol, interval, limit, intervalMinutes = 10)
            else -> emptyList()
        }
    }

    private fun find1mBars(symbol: String, limit: Int): List<MarketBar> {
        val key = RedisKeyPolicy.barsKey(symbol)
        val rawList = redisTemplate.opsForList().range(key, -limit.toLong(), -1) ?: return emptyList()
        return rawList.mapNotNull { raw ->
            runCatching {
                val state = objectMapper.readValue(raw, MinuteBarState::class.java)
                state.toMarketBar(symbol, "1m")
            }.onFailure { log.warn { "Failed to parse MinuteBarState for symbol=$symbol: ${it.message}" } }
                .getOrNull()
        }
    }

    private fun findAggregatedBars(symbol: String, interval: String, limit: Int, intervalMinutes: Int): List<MarketBar> {
        val fetchCount = limit * intervalMinutes
        val key = RedisKeyPolicy.barsKey(symbol)
        val rawList = redisTemplate.opsForList().range(key, -fetchCount.toLong(), -1) ?: return emptyList()
        val bars = rawList.mapNotNull { raw ->
            runCatching {
                objectMapper.readValue(raw, MinuteBarState::class.java)
            }.onFailure { log.warn { "Failed to parse MinuteBarState for symbol=$symbol: ${it.message}" } }
                .getOrNull()
        }
        return aggregate(bars, intervalMinutes, symbol, interval).takeLast(limit)
    }

    internal fun aggregate(
        bars: List<MinuteBarState>,
        intervalMinutes: Int,
        symbol: String,
        interval: String,
    ): List<MarketBar> {
        return bars
            .groupBy { bar ->
                val truncated = bar.startedAt.truncatedTo(ChronoUnit.HOURS)
                val minuteOfHour = ChronoUnit.MINUTES.between(truncated, bar.startedAt).toInt()
                val bucketMinute = (minuteOfHour / intervalMinutes) * intervalMinutes
                truncated.plusSeconds(bucketMinute * 60L)
            }
            .entries
            .sortedBy { it.key }
            .mapNotNull { (bucketStart, group) ->
                val sorted = group.sortedBy { it.startedAt }
                val first = sorted.first()
                val last = sorted.last()
                val totalVolume = sorted.fold(BigDecimal.ZERO) { acc, b -> acc + b.volume }
                val totalTradeValue = sorted.fold(BigDecimal.ZERO) { acc, b -> acc + b.tradeValue }
                val vwap = if (totalVolume.compareTo(BigDecimal.ZERO) == 0) {
                    BigDecimal.ZERO
                } else {
                    totalTradeValue.divide(totalVolume, 4, RoundingMode.HALF_UP)
                }
                MarketBar(
                    symbol = symbol,
                    interval = interval,
                    startedAt = bucketStart,
                    open = first.open,
                    high = sorted.maxOf { it.high },
                    low = sorted.minOf { it.low },
                    close = last.close,
                    volume = totalVolume,
                    tradeValue = totalTradeValue,
                    vwap = vwap,
                    tickCount = sorted.sumOf { it.tickCount },
                )
            }
    }

    private fun MinuteBarState.toMarketBar(symbol: String, interval: String): MarketBar {
        val vwap = if (volume.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal.ZERO
        } else {
            tradeValue.divide(volume, 4, RoundingMode.HALF_UP)
        }
        return MarketBar(
            symbol = symbol,
            interval = interval,
            startedAt = startedAt,
            open = open,
            high = high,
            low = low,
            close = close,
            volume = volume,
            tradeValue = tradeValue,
            vwap = vwap,
            tickCount = tickCount,
        )
    }
}
