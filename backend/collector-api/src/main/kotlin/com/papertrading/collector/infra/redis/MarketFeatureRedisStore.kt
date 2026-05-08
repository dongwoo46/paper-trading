package com.papertrading.collector.infra.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.collector.application.marketfeature.port.MarketFeatureStore
import com.papertrading.collector.domain.marketfeature.FeatureSnapshot
import com.papertrading.collector.domain.marketfeature.FeatureWindow
import com.papertrading.collector.domain.marketfeature.MinuteBarState
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class MarketFeatureRedisStore(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${market-feature.debug-tick-enabled:false}") private val debugTickEnabled: Boolean,
) : MarketFeatureStore {

    override fun loadCurrent(symbol: String): MinuteBarState? {
        val key = RedisKeyPolicy.currentKey(symbol)
        val data = redisTemplate.opsForHash<String, String>().entries(key)
        if (data.isEmpty()) return null

        return MinuteBarState(
            minute = data["minute"] ?: return null,
            open = (data["open"] ?: return null).toBigDecimal(),
            high = (data["high"] ?: return null).toBigDecimal(),
            low = (data["low"] ?: return null).toBigDecimal(),
            close = (data["close"] ?: return null).toBigDecimal(),
            volume = (data["volume"] ?: return null).toBigDecimal(),
            tradeValue = (data["tradeValue"] ?: return null).toBigDecimal(),
            buyVolume = (data["buyVolume"] ?: return null).toBigDecimal(),
            sellVolume = (data["sellVolume"] ?: return null).toBigDecimal(),
            tickCount = (data["tickCount"] ?: return null).toInt(),
            startedAt = java.time.Instant.parse(data["startedAt"] ?: return null),
            updatedAt = java.time.Instant.parse(data["updatedAt"] ?: return null),
        )
    }

    override fun saveCurrent(symbol: String, state: MinuteBarState) {
        val key = RedisKeyPolicy.currentKey(symbol)
        redisTemplate.opsForHash<String, String>().putAll(
            key,
            mapOf(
                "minute" to state.minute,
                "open" to state.open.toPlainString(),
                "high" to state.high.toPlainString(),
                "low" to state.low.toPlainString(),
                "close" to state.close.toPlainString(),
                "volume" to state.volume.toPlainString(),
                "tradeValue" to state.tradeValue.toPlainString(),
                "buyVolume" to state.buyVolume.toPlainString(),
                "sellVolume" to state.sellVolume.toPlainString(),
                "tickCount" to state.tickCount.toString(),
                "startedAt" to state.startedAt.toString(),
                "updatedAt" to state.updatedAt.toString(),
            ),
        )
        redisTemplate.expire(key, RedisKeyPolicy.CURRENT_TTL)
    }

    override fun appendBar(symbol: String, bar: MinuteBarState) {
        val key = RedisKeyPolicy.barsKey(symbol)
        redisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(bar))
        redisTemplate.opsForList().trim(key, -RedisKeyPolicy.BARS_MAX_LEN, -1)
        redisTemplate.expire(key, RedisKeyPolicy.BARS_TTL)
    }

    override fun loadRecentBars(symbol: String, limit: Int): List<MinuteBarState> {
        if (limit <= 0) return emptyList()
        val key = RedisKeyPolicy.barsKey(symbol)
        return redisTemplate.opsForList().range(key, -limit.toLong(), -1)
            ?.mapNotNull { raw -> runCatching { objectMapper.readValue(raw, MinuteBarState::class.java) }.getOrNull() }
            ?: emptyList()
    }

    override fun saveSnapshot(symbol: String, window: FeatureWindow, snapshot: FeatureSnapshot) {
        val key = RedisKeyPolicy.featureKey(symbol, window.name.lowercase())
        redisTemplate.opsForHash<String, String>().putAll(
            key,
            mapOf(
                "window" to snapshot.window.name.lowercase(),
                "open" to snapshot.open.toPlainString(),
                "high" to snapshot.high.toPlainString(),
                "low" to snapshot.low.toPlainString(),
                "close" to snapshot.close.toPlainString(),
                "returnRate" to snapshot.returnRate.toPlainString(),
                "volume" to snapshot.volume.toPlainString(),
                "tradeValue" to snapshot.tradeValue.toPlainString(),
                "vwap" to snapshot.vwap.toPlainString(),
                "buyVolume" to snapshot.buyVolume.toPlainString(),
                "sellVolume" to snapshot.sellVolume.toPlainString(),
                "tradeImbalance" to snapshot.tradeImbalance.toPlainString(),
                "tickCount" to snapshot.tickCount.toString(),
                "startedAt" to snapshot.startedAt.toString(),
                "updatedAt" to snapshot.updatedAt.toString(),
            ),
        )
        redisTemplate.expire(key, RedisKeyPolicy.FEATURE_TTL)
    }

    override fun loadSnapshot(symbol: String, window: FeatureWindow): FeatureSnapshot? {
        val key = RedisKeyPolicy.featureKey(symbol, window.name.lowercase())
        val data = redisTemplate.opsForHash<String, String>().entries(key)
        if (data.isEmpty()) return null
        return FeatureSnapshot(
            window = window,
            open = (data["open"] ?: return null).toBigDecimal(),
            high = (data["high"] ?: return null).toBigDecimal(),
            low = (data["low"] ?: return null).toBigDecimal(),
            close = (data["close"] ?: return null).toBigDecimal(),
            returnRate = (data["returnRate"] ?: return null).toBigDecimal(),
            volume = (data["volume"] ?: return null).toBigDecimal(),
            tradeValue = (data["tradeValue"] ?: return null).toBigDecimal(),
            vwap = (data["vwap"] ?: return null).toBigDecimal(),
            buyVolume = (data["buyVolume"] ?: return null).toBigDecimal(),
            sellVolume = (data["sellVolume"] ?: return null).toBigDecimal(),
            tradeImbalance = (data["tradeImbalance"] ?: return null).toBigDecimal(),
            tickCount = (data["tickCount"] ?: return null).toInt(),
            startedAt = java.time.Instant.parse(data["startedAt"] ?: return null),
            updatedAt = java.time.Instant.parse(data["updatedAt"] ?: return null),
        )
    }
}
