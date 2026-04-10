package com.papertrading.collector.infra.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.collector.domain.kis.KisQuoteEvent
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 시세 Redis 저장 + Pub/Sub 발행
 *
 * Hash (quote:{ticker}): 최신 시세 스냅샷 1개만 유지 (putAll = 덮어쓰기)
 *   TTL 60초 = stale 기준. Key 존재 → 신선, Key 없음 → stale.
 *   시장가/지정가 주문 접수 시 현재가 조회용.
 *
 * Pub/Sub (quote:{ticker}): 새 틱 도착 알림 → trading-api 매칭 엔진 트리거
 */
@Component
class QuoteRedisPublisher(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val QUOTE_TTL = Duration.ofSeconds(60)
        private fun quoteKey(ticker: String) = "quote:$ticker"
        private fun quoteChannel(ticker: String) = "quote:$ticker"
    }

    fun saveAndPublish(event: KisQuoteEvent) {
        val key = quoteKey(event.ticker)
        val updatedAt = event.receivedAt.toEpochMilli().toString()

        redisTemplate.opsForHash<String, String>().putAll(
            key,
            mapOf(
                "price"     to event.price.toPlainString(),
                "askp1"     to event.askp1.toPlainString(),
                "bidp1"     to event.bidp1.toPlainString(),
                "high"      to event.high.toPlainString(),
                "low"       to event.low.toPlainString(),
                "volume"    to event.volume.toPlainString(),
                "updatedAt" to updatedAt,
            ),
        )
        redisTemplate.expire(key, QUOTE_TTL)

        redisTemplate.convertAndSend(
            quoteChannel(event.ticker),
            objectMapper.writeValueAsString(
                mapOf(
                    "ticker"    to event.ticker,
                    "price"     to event.price.toPlainString(),
                    "askp1"     to event.askp1.toPlainString(),
                    "bidp1"     to event.bidp1.toPlainString(),
                    "updatedAt" to updatedAt,
                ),
            ),
        )
    }
}
