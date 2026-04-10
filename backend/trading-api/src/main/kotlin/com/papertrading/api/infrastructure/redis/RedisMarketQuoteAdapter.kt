package com.papertrading.api.infrastructure.redis

import com.papertrading.api.domain.port.MarketQuotePort
import com.papertrading.api.domain.port.QuoteSnapshot
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

@Component
class RedisMarketQuoteAdapter(
    private val redisTemplate: StringRedisTemplate,
) : MarketQuotePort {

    companion object {
        private val STALE_THRESHOLD = Duration.ofSeconds(60)
        private fun quoteKey(ticker: String) = "quote:$ticker"
    }

    override fun getQuote(ticker: String): QuoteSnapshot? {
        val hash = redisTemplate.opsForHash<String, String>().entries(quoteKey(ticker))
        if (hash.isEmpty()) return null

        val updatedAt = hash["updatedAt"]?.toLongOrNull()
            ?.let { Instant.ofEpochMilli(it) }
            ?: return null

        if (Duration.between(updatedAt, Instant.now()) > STALE_THRESHOLD) return null

        return runCatching {
            QuoteSnapshot(
                ticker = ticker,
                price = BigDecimal(hash.getValue("price")),
                askp1 = BigDecimal(hash.getValue("askp1")),
                bidp1 = BigDecimal(hash.getValue("bidp1")),
                updatedAt = updatedAt,
            )
        }.getOrNull()
    }
}