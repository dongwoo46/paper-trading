package com.papertrading.collector.application.kis.service

import com.papertrading.collector.domain.kis.SubscriptionChangeStatus
import com.papertrading.collector.infra.kis.source.ws.KisWebSocketCollector
import mu.KotlinLogging
import org.springframework.stereotype.Service

/**
 * 구독 흐름 오케스트레이터
 * DB/Redis 갱신(KisWsSubscriptionService) 완료 후 WebSocket emit(KisWebSocketCollector) 순서 보장.
 */
@Service
class KisSubscriptionOrchestrator(
    private val subscriptionService: KisWsSubscriptionService,
    private val webSocketCollector: KisWebSocketCollector,
) {
    private val log = KotlinLogging.logger {}

    fun subscribe(mode: String, ticker: String): SubscriptionChangeStatus {
        val status = subscriptionService.addSymbol(mode, ticker)
        if (status == SubscriptionChangeStatus.ADDED) {
            webSocketCollector.emit(mode, listOf(ticker), subscribe = true)
            log.info { "subscribed: ticker=$ticker, mode=$mode" }
        }
        return status
    }

    fun unsubscribe(mode: String, ticker: String): SubscriptionChangeStatus {
        val status = subscriptionService.removeSymbol(mode, ticker)
        if (status == SubscriptionChangeStatus.REMOVED) {
            webSocketCollector.emit(mode, listOf(ticker), subscribe = false)
            log.info { "unsubscribed: ticker=$ticker, mode=$mode" }
        }
        return status
    }
}
