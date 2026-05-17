package com.papertrading.api.infrastructure.redis

import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.PriceSource
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.port.LivePositionCachePort
import com.papertrading.api.domain.port.LivePositionSnapshot
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

@Component
class RedisLivePositionCacheAdapter(
    private val redisTemplate: StringRedisTemplate,
) : LivePositionCachePort {

    companion object {
        private fun positionKey(accountId: Long, ticker: String) = "position:live:$accountId:${ticker.uppercase()}"
        private fun accountIndexKey(accountId: Long) = "position:live:index:account:$accountId"
        private fun tickerIndexKey(ticker: String) = "position:live:index:ticker:${ticker.uppercase()}"
        private fun tickerModeIndexKey(ticker: String, tradingMode: TradingMode) =
            "position:live:index:ticker:${ticker.uppercase()}:mode:${tradingMode.name}"
    }

    override fun save(snapshot: LivePositionSnapshot): LivePositionSnapshot {
        val key = positionKey(snapshot.accountId, snapshot.ticker)
        redisTemplate.opsForHash<String, String>().putAll(key, snapshot.toHash())
        redisTemplate.opsForSet().add(accountIndexKey(snapshot.accountId), key)
        redisTemplate.opsForSet().add(tickerIndexKey(snapshot.ticker), key)
        redisTemplate.opsForSet().add(tickerModeIndexKey(snapshot.ticker, snapshot.tradingMode), key)
        return snapshot
    }

    override fun find(accountId: Long, ticker: String): LivePositionSnapshot? =
        read(positionKey(accountId, ticker))

    override fun findByAccountId(accountId: Long): List<LivePositionSnapshot> =
        redisTemplate.opsForSet().members(accountIndexKey(accountId))
            .orEmpty()
            .mapNotNull { read(it) }
            .filter { it.quantity > BigDecimal.ZERO }
            .sortedBy { it.ticker }

    override fun findByTicker(ticker: String): List<LivePositionSnapshot> =
        redisTemplate.opsForSet().members(tickerIndexKey(ticker))
            .orEmpty()
            .mapNotNull { read(it) }
            .filter { it.quantity > BigDecimal.ZERO }

    override fun findByTickerAndMode(ticker: String, tradingMode: TradingMode): List<LivePositionSnapshot> =
        redisTemplate.opsForSet().members(tickerModeIndexKey(ticker, tradingMode))
            .orEmpty()
            .mapNotNull { read(it) }
            .filter { it.quantity > BigDecimal.ZERO }

    private fun read(key: String): LivePositionSnapshot? {
        val hash = redisTemplate.opsForHash<String, String>().entries(key)
        if (hash.isEmpty()) return null
        return runCatching {
            LivePositionSnapshot(
                id = hash.getValue("id").toLong(),
                accountId = hash.getValue("accountId").toLong(),
                tradingMode = hash["tradingMode"]?.let { TradingMode.valueOf(it) } ?: TradingMode.LOCAL,
                ticker = hash.getValue("ticker"),
                marketType = MarketType.valueOf(hash.getValue("marketType")),
                quantity = BigDecimal(hash.getValue("quantity")),
                orderableQuantity = BigDecimal(hash.getValue("orderableQuantity")),
                lockedQuantity = BigDecimal(hash.getValue("lockedQuantity")),
                avgBuyPrice = BigDecimal(hash.getValue("avgBuyPrice")),
                totalBuyAmount = BigDecimal(hash.getValue("totalBuyAmount")),
                currentPrice = hash["currentPrice"]?.toBigDecimalOrNull(),
                evaluationAmount = hash["evaluationAmount"]?.toBigDecimalOrNull(),
                unrealizedPnl = hash["unrealizedPnl"]?.toBigDecimalOrNull(),
                returnRate = hash["returnRate"]?.toBigDecimalOrNull(),
                priceSource = PriceSource.valueOf(hash.getValue("priceSource")),
                priceUpdatedAt = hash["priceUpdatedAt"]?.toLongOrNull()?.let { Instant.ofEpochMilli(it) },
            )
        }.getOrNull()
    }

    private fun LivePositionSnapshot.toHash(): Map<String, String> =
        buildMap {
            put("id", id.toString())
            put("accountId", accountId.toString())
            put("tradingMode", tradingMode.name)
            put("ticker", ticker)
            put("marketType", marketType.name)
            put("quantity", quantity.toPlainString())
            put("orderableQuantity", orderableQuantity.toPlainString())
            put("lockedQuantity", lockedQuantity.toPlainString())
            put("avgBuyPrice", avgBuyPrice.toPlainString())
            put("totalBuyAmount", totalBuyAmount.toPlainString())
            currentPrice?.let { put("currentPrice", it.toPlainString()) }
            evaluationAmount?.let { put("evaluationAmount", it.toPlainString()) }
            unrealizedPnl?.let { put("unrealizedPnl", it.toPlainString()) }
            returnRate?.let { put("returnRate", it.toPlainString()) }
            put("priceSource", priceSource.name)
            priceUpdatedAt?.let { put("priceUpdatedAt", it.toEpochMilli().toString()) }
        }
}
