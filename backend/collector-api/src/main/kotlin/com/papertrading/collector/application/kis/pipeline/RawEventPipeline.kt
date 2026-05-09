package com.papertrading.collector.application.kis.pipeline

import com.papertrading.collector.application.marketfeature.service.MarketFeatureAggregationService
import com.papertrading.collector.infra.redis.QuoteRedisPublisher
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class RawEventPipeline(
    private val parser: KisRawEventParser,
    private val orderbookParser: KisOrderbookEventParser,
    private val publisher: QuoteRedisPublisher,
    private val marketFeatureAggregationService: MarketFeatureAggregationService,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val TR_ID_CONTRACT  = "H0STCNT0"
        private const val TR_ID_ORDERBOOK = "H0STASP0"
    }

    fun publish(source: String, payload: String, receivedAt: Instant = Instant.now()) {
        val parts = payload.split("|", limit = 4)
        if (parts.size < 2) return

        when (parts[1]) {
            TR_ID_CONTRACT -> publishQuote(source, payload)
            TR_ID_ORDERBOOK -> publishOrderbook(source, payload)
            else -> log.debug { "알 수 없는 TR_ID: ${parts[1]}, source=$source" }
        }
    }

    private fun publishQuote(source: String, payload: String) {
        val event = parser.parse(payload) ?: return
        publisher.saveAndPublish(event)
        runCatching { marketFeatureAggregationService.onTick(event) }
            .onFailure { ex ->
                log.warn(ex) { "market feature aggregation failed: source=$source, ticker=${event.ticker}" }
            }
        log.debug { "quote published: source=$source, ticker=${event.ticker}, price=${event.price}" }
    }

    private fun publishOrderbook(source: String, payload: String) {
        val event = orderbookParser.parse(payload) ?: return
        // step-3에서 OrderbookRedisStore.save() + OrderbookIngestMetrics.record() 연결 예정
        log.debug { "orderbook received: source=$source, ticker=${event.ticker}" }
    }
}
