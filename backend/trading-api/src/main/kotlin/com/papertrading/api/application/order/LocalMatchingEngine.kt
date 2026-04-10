package com.papertrading.api.application.order

import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderType
import com.papertrading.api.domain.model.Order
import com.papertrading.api.domain.port.QuoteSnapshot
import com.papertrading.api.infrastructure.persistence.OrderRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.math.BigDecimal

/**
 * LOCAL 모드 체결 엔진
 * 트리거: (1) QuoteEventListener — 새 틱 수신, (2) OrderCommandService — 주문 접수 직후 초기 매칭
 * 각 주문 체결은 ExecutionProcessor.fill()이 독립 트랜잭션으로 처리.
 */
@Service
class LocalMatchingEngine(
    private val orderRepository: OrderRepository,
    private val executionProcessor: ExecutionProcessor,
) {
    private val log = KotlinLogging.logger {}

    /** ticker의 모든 PENDING/PARTIAL LOCAL 주문에 대해 매칭 시도 */
    fun tryMatchPendingOrders(ticker: String, quote: QuoteSnapshot) {
        val orders = orderRepository.findActiveLocalOrdersByTicker(ticker)
        for (order in orders) {
            runCatching { tryMatchOne(order, quote) }
                .onFailure { log.warn { "매칭 실패: orderId=${order.id}, reason=${it.message}" } }
        }
    }

    /** 단일 주문 매칭 시도 */
    fun tryMatchOne(order: Order, quote: QuoteSnapshot) {
        val orderId = order.id ?: return
        val fillPrice = determineFillPrice(order, quote) ?: return
        val remainQty = order.quantity.subtract(order.filledQuantity)
        executionProcessor.fill(orderId, fillPrice, remainQty)
    }

    /**
     * 체결가 결정 (null = 체결 불가)
     * MARKET: 현재 체결가(price)로 즉시 체결
     * LIMIT BUY:  askp1 <= limitPrice → 체결가 = limitPrice
     * LIMIT SELL: bidp1 >= limitPrice → 체결가 = limitPrice
     */
    fun determineFillPrice(order: Order, quote: QuoteSnapshot): BigDecimal? {
        val limitPrice = order.limitPrice
        return when (order.orderType) {
            OrderType.MARKET -> quote.price
            OrderType.LIMIT -> when (order.orderSide) {
                OrderSide.BUY -> limitPrice?.takeIf { quote.askp1 <= it }
                OrderSide.SELL -> limitPrice?.takeIf { quote.bidp1 >= it }
                null -> null
            }
            null -> null
        }
    }
}
