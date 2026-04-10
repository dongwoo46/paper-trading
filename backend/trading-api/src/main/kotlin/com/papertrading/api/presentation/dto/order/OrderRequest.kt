package com.papertrading.api.presentation.dto.order

import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderType
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant

data class PlaceOrderRequest(
    @field:NotBlank val ticker: String,
    @field:NotNull val marketType: MarketType,
    @field:NotNull val orderType: OrderType,
    @field:NotNull val orderSide: OrderSide,
    @field:NotNull val orderCondition: OrderCondition,
    @field:NotNull @field:DecimalMin("0.00000001") val quantity: BigDecimal,
    val limitPrice: BigDecimal?,   // LIMIT 주문 시 필수
    val expireAt: Instant?,        // GTD 주문 시 필수
    @field:NotBlank val idempotencyKey: String,
    val strategyId: Long? = null,
    val signalId: Long? = null,
)
