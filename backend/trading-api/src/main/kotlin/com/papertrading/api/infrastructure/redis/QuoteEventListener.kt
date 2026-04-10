package com.papertrading.api.infrastructure.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.api.application.order.LocalMatchingEngine
import com.papertrading.api.domain.port.QuoteSnapshot
import mu.KotlinLogging
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

/**
 * Redis Pub/Sub 시세 이벤트 수신기
 * collector-api가 quote:{ticker} 채널에 발행한 메시지를 수신해 LocalMatchingEngine 트리거.
 * 메시지 형식: {"ticker":"...","price":"...","askp1":"...","bidp1":"...","updatedAt":...}
 */
@Component
class QuoteEventListener(
    private val localMatchingEngine: LocalMatchingEngine,
    private val objectMapper: ObjectMapper,
) : MessageListener {

    private val log = KotlinLogging.logger {}

    override fun onMessage(message: Message, pattern: ByteArray?) {
        val quote = parseMessage(message.body) ?: return
        runCatching { localMatchingEngine.tryMatchPendingOrders(quote.ticker, quote) }
            .onFailure { log.warn { "매칭 처리 오류: ticker=${quote.ticker}, reason=${it.message}" } }
    }

    private fun parseMessage(body: ByteArray): QuoteSnapshot? = runCatching {
        val map = objectMapper.readValue(body, Map::class.java)
        QuoteSnapshot(
            ticker = map["ticker"] as? String ?: return@runCatching null,
            price = BigDecimal(map["price"] as? String ?: return@runCatching null),
            askp1 = BigDecimal(map["askp1"] as? String ?: return@runCatching null),
            bidp1 = BigDecimal(map["bidp1"] as? String ?: return@runCatching null),
            updatedAt = (map["updatedAt"] as? Number)?.toLong()
                ?.let { Instant.ofEpochMilli(it) } ?: return@runCatching null,
        )
    }.getOrElse {
        log.warn { "quote 메시지 파싱 실패: ${it.message}" }
        null
    }
}