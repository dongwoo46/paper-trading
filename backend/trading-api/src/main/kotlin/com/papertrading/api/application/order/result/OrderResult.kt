package com.papertrading.api.application.order.result

import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.enums.OrderType
import java.math.BigDecimal
import java.time.Instant

data class OrderResult(
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
        fun from(order: Order) = OrderResult(
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

