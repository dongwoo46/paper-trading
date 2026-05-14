package com.papertrading.api.application.order

import com.papertrading.api.application.notification.SlackNotificationEventPublisher
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.infrastructure.kis.KisOrderRestClient
import com.papertrading.api.infrastructure.persistence.OrderRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * KIS_LIVE 주문 실행기
 * KIS_PAPER와 동일 구조. live 엔드포인트 + TR_ID 사용.
 * 체결 확인: KisPaperPollingScheduler가 통합 처리.
 */
@Service
class KisLiveOrderExecutor(
    private val orderRepository: OrderRepository,
    private val kisOrderRestClient: KisOrderRestClient,
    private val slackNotificationEventPublisher: SlackNotificationEventPublisher,
) {
    private val log = KotlinLogging.logger {}

    // slack 알림서비스 추가, after commit 이후 slack메시지 발생
    @Transactional
    fun submit(order: Order) {
        val orderId = order.id ?: return
        val accountId = order.account?.id ?: return
        val ticker = order.ticker ?: "UNKNOWN"
        runCatching {
            val orno = kisOrderRestClient.placeOrder(order, "live")
            order.assignExternalOrderId(orno)
            log.info { "KIS_LIVE 주문 접수: orderId=$orderId, orno=$orno, ticker=${order.ticker}" }
            slackNotificationEventPublisher.publishOrderCreated(
                accountId = accountId,
                orderId = orderId,
                message = "KIS_LIVE order created: orderId=$orderId, ticker=$ticker, externalOrderId=$orno",
            )
        }.onFailure {
            log.error { "KIS_LIVE 주문 접수 실패: orderId=$orderId, reason=${it.message}" }
            throw it
        }
    }

    // slack 알림서비스 추가, after commit 이후 slack메시지 발생
    @Transactional
    fun cancel(order: Order) {
        val orderId = order.id ?: return
        val accountId = order.account?.id ?: return
        val ticker = order.ticker ?: "UNKNOWN"
        runCatching {
            kisOrderRestClient.cancelOrder(order, "live")
            log.info { "KIS_LIVE 주문 취소: orderId=$orderId" }
            slackNotificationEventPublisher.publishOrderCanceled(
                accountId = accountId,
                orderId = orderId,
                message = "KIS_LIVE order canceled: orderId=$orderId, ticker=$ticker",
            )
        }.onFailure {
            log.warn { "KIS_LIVE 주문 취소 실패: orderId=$orderId, reason=${it.message}" }
        }
    }
}
