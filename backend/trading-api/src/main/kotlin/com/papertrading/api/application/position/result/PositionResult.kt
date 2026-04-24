package com.papertrading.api.application.position.result

import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.PriceSource
import com.papertrading.api.domain.model.Position
import java.math.BigDecimal
import java.time.Instant

data class PositionResult(
    val id: Long,
    val accountId: Long,
    val ticker: String,
    val marketType: MarketType,
    val quantity: BigDecimal,
    val orderableQuantity: BigDecimal,
    val lockedQuantity: BigDecimal,
    val avgBuyPrice: BigDecimal,
    val totalBuyAmount: BigDecimal,
    val currentPrice: BigDecimal?,
    val evaluationAmount: BigDecimal?,
    val unrealizedPnl: BigDecimal?,
    val returnRate: BigDecimal?,
    val priceSource: PriceSource,
    val priceUpdatedAt: Instant?,
) {
    companion object {
        fun from(p: Position): PositionResult = PositionResult(
            id = requireNotNull(p.id) { "position.id is null" },
            accountId = requireNotNull(p.account?.id) { "position.account.id is null" },
            ticker = requireNotNull(p.ticker) { "position.ticker is null" },
            marketType = requireNotNull(p.marketType) { "position.marketType is null" },
            quantity = p.quantity,
            orderableQuantity = p.orderableQuantity,
            lockedQuantity = p.lockedQuantity,
            avgBuyPrice = p.avgBuyPrice,
            totalBuyAmount = p.totalBuyAmount,
            currentPrice = p.currentPrice,
            evaluationAmount = p.evaluationAmount,
            unrealizedPnl = p.unrealizedPnl,
            returnRate = p.returnRate,
            priceSource = p.priceSource,
            priceUpdatedAt = p.priceUpdatedAt,
        )
    }
}