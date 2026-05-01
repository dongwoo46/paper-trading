package com.papertrading.api.domain.event

import com.papertrading.api.domain.enums.OrderSide
import java.math.BigDecimal
import java.time.Instant

data class ExecutionFilledEvent(
    val executionId: Long,
    val orderId: Long,
    val ticker: String,
    val side: OrderSide,
    val quantity: BigDecimal,
    val price: BigDecimal,
    val fee: BigDecimal,
    val currency: String,
    val executedAt: Instant,
)
