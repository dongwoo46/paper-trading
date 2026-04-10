package com.papertrading.api.application.order.command

import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderType
import java.math.BigDecimal
import java.time.Instant

data class PlaceOrderCommand(
    val ticker: String,
    val marketType: MarketType,
    val orderType: OrderType,
    val orderSide: OrderSide,
    val orderCondition: OrderCondition,
    val quantity: BigDecimal,
    val limitPrice: BigDecimal?,   // LIMIT 주문 시 필수
    val expireAt: Instant?,        // GTD 주문 시 필수
    val idempotencyKey: String,
    val strategyId: Long? = null,
    val signalId: Long? = null,
)
