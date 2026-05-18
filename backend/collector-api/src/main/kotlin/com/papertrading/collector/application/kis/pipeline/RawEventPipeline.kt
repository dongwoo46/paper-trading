package com.papertrading.collector.application.kis.pipeline

import com.papertrading.collector.application.kis.service.OrderbookIngestMetrics
import com.papertrading.collector.application.marketfeature.service.MarketFeatureAggregationService
import com.papertrading.collector.infra.redis.OrderbookRedisStore
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
    private val orderbookRedisStore: OrderbookRedisStore,
    private val orderbookIngestMetrics: OrderbookIngestMetrics,
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
        val mode = modeFromSource(source) ?: return
        val modeEvent = event.copy(mode = mode)
        publisher.saveAndPublish(modeEvent)
        runCatching { marketFeatureAggregationService.onTick(modeEvent) }
            .onFailure { ex ->
                log.warn(ex) { "market feature aggregation failed: source=$source, mode=$mode, ticker=${event.ticker}" }
            }
        log.debug { "quote published: source=$source, mode=$mode, ticker=${event.ticker}, price=${event.price}" }
    }

    private fun publishOrderbook(source: String, payload: String) {
        orderbookIngestMetrics.recordReceived()
        val event = orderbookParser.parse(payload)
        if (event == null) {
            orderbookIngestMetrics.recordParseFail()
            return
        }
        runCatching { orderbookRedisStore.save(event) }
            .onFailure { ex ->
                log.warn(ex) { "orderbook redis store failed: source=$source, ticker=${event.ticker}" }
            }
        log.debug { "orderbook stored: source=$source, ticker=${event.ticker}" }
    }

    private fun modeFromSource(source: String): String? = when (source.trim().lowercase()) {
        "kis-paper", "paper" -> "paper"
        "kis-live", "live" -> "live"
        else -> null
    }
}
