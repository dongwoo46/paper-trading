package com.papertrading.api.domain.port

import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.PriceSource
import com.papertrading.api.domain.enums.TradingMode
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

data class LivePositionSnapshot(
    val id: Long,
    val accountId: Long,
    val tradingMode: TradingMode,
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
        fun fromPosition(position: Position): LivePositionSnapshot =
            LivePositionSnapshot(
                id = requireNotNull(position.id) { "position.id is null" },
                accountId = requireNotNull(position.account.id) { "position.account.id is null" },
                tradingMode = position.account.tradingMode,
                ticker = position.ticker,
                marketType = position.marketType,
                quantity = position.quantity,
                orderableQuantity = position.orderableQuantity,
                lockedQuantity = position.lockedQuantity,
                avgBuyPrice = position.avgBuyPrice,
                totalBuyAmount = position.totalBuyAmount,
                currentPrice = position.currentPrice,
                evaluationAmount = position.evaluationAmount,
                unrealizedPnl = position.unrealizedPnl,
                returnRate = position.returnRate,
                priceSource = position.priceSource,
                priceUpdatedAt = position.priceUpdatedAt,
            )

        fun evaluate(
            position: Position,
            price: BigDecimal,
            source: PriceSource,
            updatedAt: Instant,
        ): LivePositionSnapshot =
            fromPosition(position).evaluate(price, source, updatedAt)
    }

    fun evaluate(price: BigDecimal, source: PriceSource, updatedAt: Instant): LivePositionSnapshot {
        require(price >= BigDecimal.ZERO) { "현재가는 0 이상이어야 합니다." }
        val evaluatedAmount = price.multiply(quantity)
        return copy(
            currentPrice = price,
            evaluationAmount = evaluatedAmount,
            unrealizedPnl = evaluatedAmount.subtract(totalBuyAmount),
            returnRate = if (avgBuyPrice > BigDecimal.ZERO) {
                price.subtract(avgBuyPrice).divide(avgBuyPrice, 4, RoundingMode.HALF_UP)
            } else {
                null
            },
            priceSource = source,
            priceUpdatedAt = updatedAt,
        )
    }
}
