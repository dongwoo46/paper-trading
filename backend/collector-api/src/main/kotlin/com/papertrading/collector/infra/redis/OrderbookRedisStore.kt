package com.papertrading.collector.infra.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.collector.domain.entity.kis.KisOrderbookEvent
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

@Component
class OrderbookRedisStore(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {

    fun save(event: KisOrderbookEvent) {
        val bestAsk = event.askPrices[0]
        val bestBid = event.bidPrices[0]
        val spread = bestAsk - bestBid

        val askDepthTopN = event.askQtys.fold(BigDecimal.ZERO, BigDecimal::add)
        val bidDepthTopN = event.bidQtys.fold(BigDecimal.ZERO, BigDecimal::add)

        val denominator = bidDepthTopN + askDepthTopN
        val depthImbalance = if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            null
        } else {
            (bidDepthTopN - askDepthTopN).divide(denominator, 8, RoundingMode.HALF_UP)
        }

        val bidLevels = event.bidPrices.zip(event.bidQtys).map { (price, qty) ->
            mapOf("price" to price.toPlainString(), "qty" to qty.toPlainString())
        }
        val askLevels = event.askPrices.zip(event.askQtys).map { (price, qty) ->
            mapOf("price" to price.toPlainString(), "qty" to qty.toPlainString())
        }

        val fields = mutableMapOf(
            "bestBid" to bestBid.toPlainString(),
            "bestAsk" to bestAsk.toPlainString(),
            "spread" to spread.toPlainString(),
            "bidDepthTopN" to bidDepthTopN.toPlainString(),
            "askDepthTopN" to askDepthTopN.toPlainString(),
            "bidLevels" to objectMapper.writeValueAsString(bidLevels),
            "askLevels" to objectMapper.writeValueAsString(askLevels),
            "source" to "H0STASP0",
            "timestamp" to event.receivedAt.toString(),
        )
        if (depthImbalance != null) {
            fields["depthImbalance"] = depthImbalance.toPlainString()
        }

        val key = RedisKeyPolicy.orderbookKey(event.ticker)
        redisTemplate.opsForHash<String, String>().putAll(key, fields)
        redisTemplate.expire(key, RedisKeyPolicy.ORDERBOOK_TTL)
    }

    fun load(symbol: String): OrderbookSnapshot? {
        val key = RedisKeyPolicy.orderbookKey(symbol)
        val data = redisTemplate.opsForHash<String, String>().entries(key)
        if (data.isEmpty()) return null

        val timestamp = data["timestamp"]?.let { raw ->
            runCatching { java.time.Instant.parse(raw) }.getOrNull()
        }

        return OrderbookSnapshot(
            bestBid = data["bestBid"]?.toBigDecimalOrNull(),
            bestAsk = data["bestAsk"]?.toBigDecimalOrNull(),
            spread = data["spread"]?.toBigDecimalOrNull(),
            bidDepthTopN = data["bidDepthTopN"]?.toBigDecimalOrNull(),
            askDepthTopN = data["askDepthTopN"]?.toBigDecimalOrNull(),
            depthImbalance = data["depthImbalance"]?.toBigDecimalOrNull(),
            timestamp = timestamp,
        )
    }
}
