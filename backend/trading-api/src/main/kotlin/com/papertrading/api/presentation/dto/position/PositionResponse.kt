package com.papertrading.api.presentation.dto.position

import com.papertrading.api.application.position.result.PositionResult
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.PriceSource
import java.math.BigDecimal
import java.time.Instant

data class PositionResponse(
    val ticker: String,
    val marketType: MarketType,
    val quantity: BigDecimal,
    val orderableQuantity: BigDecimal,
    val lockedQuantity: BigDecimal,
    val avgBuyPrice: BigDecimal,
    val currentPrice: BigDecimal?,
    val evaluationAmount: BigDecimal?,
    val unrealizedPnl: BigDecimal?,
    val returnRate: BigDecimal?,
    val priceSource: PriceSource,
    val priceUpdatedAt: Instant?,
) {
    companion object {
        fun from(r: PositionResult) = PositionResponse(
            ticker = r.ticker,
            marketType = r.marketType,
            quantity = r.quantity,
            orderableQuantity = r.orderableQuantity,
            lockedQuantity = r.lockedQuantity,
            avgBuyPrice = r.avgBuyPrice,
            currentPrice = r.currentPrice,
            evaluationAmount = r.evaluationAmount,
            unrealizedPnl = r.unrealizedPnl,
            returnRate = r.returnRate,
            priceSource = r.priceSource,
            priceUpdatedAt = r.priceUpdatedAt,
        )
    }
}
