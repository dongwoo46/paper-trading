package com.papertrading.api.application.order

import com.papertrading.api.application.notification.SlackNotificationEventPublisher
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.infrastructure.kis.KisOrderRestClient
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * KIS_PAPER 주문 실행기
 * 주문 접수: KIS 모의투자 REST API → externalOrderId 저장.
 * 체결 확인: KIS WebSocket 체결 통지(KisExecutionNoticeService)로 처리.
 */
@Service
class KisPaperOrderExecutor(
    private val kisOrderRestClient: KisOrderRestClient,
    private val slackNotificationEventPublisher: SlackNotificationEventPublisher,
) {
    private val log = KotlinLogging.logger {}

    @Transactional
    fun submit(order: Order) {
        val orderId = order.id ?: return
        val accountId = order.account?.id ?: return
        val ticker = order.ticker ?: "UNKNOWN"
        runCatching {
            val orno = kisOrderRestClient.placeOrder(order, "paper")
            order.assignExternalOrderId(orno)
            log.info { "KIS_PAPER 주문 접수: orderId=$orderId, orno=$orno, ticker=${order.ticker}" }
            slackNotificationEventPublisher.publishOrderCreated(
                accountId = accountId,
                orderId = orderId,
                message = "KIS_PAPER order created: orderId=$orderId, ticker=$ticker, externalOrderId=$orno",
            )
        }.onFailure {
            log.error { "KIS_PAPER 주문 접수 실패: orderId=$orderId, reason=${it.message}" }
            throw it
        }
    }

    @Transactional
    fun cancel(order: Order) {
        val orderId = order.id ?: return
        val accountId = order.account?.id ?: return
        val ticker = order.ticker ?: "UNKNOWN"
        runCatching {
            kisOrderRestClient.cancelOrder(order, "paper")
            log.info { "KIS_PAPER 주문 취소: orderId=$orderId" }
            slackNotificationEventPublisher.publishOrderCanceled(
                accountId = accountId,
                orderId = orderId,
                message = "KIS_PAPER order canceled: orderId=$orderId, ticker=$ticker",
            )
        }.onFailure {
            log.warn { "KIS_PAPER 주문 취소 실패: orderId=$orderId, reason=${it.message}" }
        }
    }

    // 웹소켓 체결 통지로 전환되어 폴링은 사용하지 않는다.
    fun pollFills() = Unit
}
