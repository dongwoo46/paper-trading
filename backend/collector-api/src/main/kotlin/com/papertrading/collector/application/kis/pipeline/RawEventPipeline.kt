package com.papertrading.collector.application.kis.pipeline

import com.papertrading.collector.application.marketfeature.service.MarketFeatureAggregationService
import com.papertrading.collector.infra.redis.QuoteRedisPublisher
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class RawEventPipeline(
    private val parser: KisRawEventParser,
    private val publisher: QuoteRedisPublisher,
    private val marketFeatureAggregationService: MarketFeatureAggregationService,
) {
    private val log = KotlinLogging.logger {}

    fun publish(source: String, payload: String, receivedAt: Instant = Instant.now()) {
        val event = parser.parse(payload) ?: return
        publisher.saveAndPublish(event)
        runCatching { marketFeatureAggregationService.onTick(event) }
            .onFailure { ex ->
                log.warn(ex) { "market feature aggregation failed: source=$source, ticker=${event.ticker}" }
            }
        log.debug { "quote published: source=$source, ticker=${event.ticker}, price=${event.price}" }
    }
}


