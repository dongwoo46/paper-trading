package com.papertrading.api.application.order

import com.papertrading.api.domain.model.Order
import com.papertrading.api.infrastructure.kis.KisOrderRestClient
import com.papertrading.api.infrastructure.persistence.OrderRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * KIS_PAPER 주문 실행기
 * 주문 접수: KIS 모의투자 REST API → externalOrderId 저장.
 * 체결 확인: pollFills() — KisPaperPollingScheduler가 3초 간격으로 호출.
 */
@Service
class KisPaperOrderExecutor(
    private val orderRepository: OrderRepository,
    private val kisOrderRestClient: KisOrderRestClient,
    private val executionProcessor: ExecutionProcessor,
) {
    private val log = KotlinLogging.logger {}

    @Transactional
    fun submit(order: Order) {
        val orderId = order.id ?: return
        runCatching {
            val orno = kisOrderRestClient.placeOrder(order, "paper")
            order.externalOrderId = orno
            log.info { "KIS_PAPER 주문 접수: orderId=$orderId, orno=$orno, ticker=${order.ticker}" }
        }.onFailure {
            log.error { "KIS_PAPER 주문 접수 실패: orderId=$orderId, reason=${it.message}" }
            throw it
        }
    }

    @Transactional
    fun cancel(order: Order) {
        val orderId = order.id ?: return
        runCatching {
            kisOrderRestClient.cancelOrder(order, "paper")
            log.info { "KIS_PAPER 주문 취소: orderId=$orderId" }
        }.onFailure {
            log.warn { "KIS_PAPER 주문 취소 실패: orderId=$orderId, reason=${it.message}" }
        }
    }

    /** KIS_PAPER 미체결 주문 체결 확인 — 스케줄러에서 3초마다 호출 */
    fun pollFills() {
        val orders = orderRepository.findPendingKisPaperOrders()
        if (orders.isEmpty()) return

        for (order in orders) {
            val orderId = order.id ?: continue
            runCatching {
                val fills = kisOrderRestClient.inquireFills(order, "paper")
                for (fill in fills) {
                    if (fill.executedQty <= BigDecimal.ZERO) continue
                    val newQty = fill.executedQty.subtract(order.filledQuantity)
                    if (newQty <= BigDecimal.ZERO) continue
                    executionProcessor.fill(orderId, fill.executedPrice, newQty)
                    log.info { "KIS_PAPER 체결: orderId=$orderId, qty=$newQty, price=${fill.executedPrice}" }
                }
            }.onFailure {
                log.warn { "KIS_PAPER 체결 조회 실패: orderId=$orderId, reason=${it.message}" }
            }
        }
    }
}