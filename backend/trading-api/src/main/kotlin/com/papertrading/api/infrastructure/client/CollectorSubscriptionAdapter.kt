package com.papertrading.api.infrastructure.client

import com.papertrading.api.domain.port.CollectorSubscriptionPort
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * collector-api 내부 구독 API 클라이언트
 * POST/DELETE /api/internal/subscriptions/{ticker}?mode=
 * 실패 시 로그만 남기고 진행 (구독 실패가 주문 처리를 막지 않도록).
 */
@Component
class CollectorSubscriptionAdapter(
    private val properties: CollectorApiProperties,
) : CollectorSubscriptionPort {

    private val log = KotlinLogging.logger {}
    private val restTemplate = RestTemplate()

    override fun subscribe(mode: String, ticker: String) {
        val url = "${properties.baseUrl}/api/internal/subscriptions/$ticker?mode=$mode"
        runCatching { restTemplate.postForObject(url, null, String::class.java) }
            .onSuccess { log.debug { "collector subscribe: ticker=$ticker, mode=$mode" } }
            .onFailure { log.warn { "collector 구독 요청 실패: ticker=$ticker, mode=$mode, reason=${it.message}" } }
    }

    override fun unsubscribe(mode: String, ticker: String) {
        val url = "${properties.baseUrl}/api/internal/subscriptions/$ticker?mode=$mode"
        runCatching { restTemplate.delete(url) }
            .onSuccess { log.debug { "collector unsubscribe: ticker=$ticker, mode=$mode" } }
            .onFailure { log.warn { "collector 구독 해제 요청 실패: ticker=$ticker, mode=$mode, reason=${it.message}" } }
    }
}
