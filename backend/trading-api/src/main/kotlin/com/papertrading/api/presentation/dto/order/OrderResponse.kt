package com.papertrading.api.presentation.dto.order

import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.enums.OrderType
import com.papertrading.api.domain.model.Execution
import com.papertrading.api.domain.model.Order
import java.math.BigDecimal
import java.time.Instant

data class OrderResponse(
    val orderId: Long,
    val ticker: String,
    val marketType: MarketType,
    val orderType: OrderType,
    val orderSide: OrderSide,
    val orderCondition: OrderCondition,
    val orderStatus: OrderStatus,
    val quantity: BigDecimal,
    val filledQuantity: BigDecimal,
    val limitPrice: BigDecimal?,
    val avgFilledPrice: BigDecimal?,
    val fee: BigDecimal,
    val createdAt: Instant,
) {
    companion object {
        fun from(order: Order) = OrderResponse(
            orderId = requireNotNull(order.id) { "order.id is null" },
            ticker = requireNotNull(order.ticker) { "order.ticker is null" },
            marketType = requireNotNull(order.marketType) { "order.marketType is null" },
            orderType = requireNotNull(order.orderType) { "order.orderType is null" },
            orderSide = requireNotNull(order.orderSide) { "order.orderSide is null" },
            orderCondition = requireNotNull(order.orderCondition) { "order.orderCondition is null" },
            orderStatus = order.orderStatus,
            quantity = order.quantity,
            filledQuantity = order.filledQuantity,
            limitPrice = order.limitPrice,
            avgFilledPrice = order.avgFilledPrice,
            fee = order.fee,
            createdAt = requireNotNull(order.createdAt) { "order.createdAt is null" },
        )
    }
}

data class ExecutionResponse(
    val executionId: Long,
    val executedQuantity: BigDecimal,
    val executedPrice: BigDecimal,
    val fee: BigDecimal,
    val executedAt: Instant,
) {
    companion object {
        fun from(e: Execution) = ExecutionResponse(
            executionId = requireNotNull(e.id) { "execution.id is null" },
            executedQuantity = e.executedQuantity,
            executedPrice = e.executedPrice,
            fee = e.fee,
            executedAt = requireNotNull(e.executedAt) { "execution.executedAt is null" },
        )
    }
}
