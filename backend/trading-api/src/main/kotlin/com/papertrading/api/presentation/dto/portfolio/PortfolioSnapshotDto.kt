package com.papertrading.api.presentation.dto.portfolio

import com.papertrading.api.domain.entity.portfolio.DailyBalance
import com.papertrading.api.domain.entity.portfolio.PortfolioSnapshot
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

data class DailyBalanceResponse(
    val businessDate: LocalDate,
    val cashBalance: BigDecimal,
    val stockMarketValue: BigDecimal,
    val totalAssetValue: BigDecimal,
    val pnlAmount: BigDecimal,
    val pnlRate: BigDecimal,
) {
    companion object {
        fun from(entity: DailyBalance): DailyBalanceResponse = DailyBalanceResponse(
            businessDate = entity.businessDate,
            cashBalance = entity.cashBalance,
            stockMarketValue = entity.stockMarketValue,
            totalAssetValue = entity.totalAssetValue,
            pnlAmount = entity.pnlAmount,
            pnlRate = entity.pnlRate.setScale(6, RoundingMode.HALF_UP),
        )
    }
}

data class PortfolioSnapshotResponse(
    val businessDate: LocalDate,
    val ticker: String,
    val quantity: BigDecimal,
    val avgBuyPrice: BigDecimal,
    val closePrice: BigDecimal,
    val marketValue: BigDecimal,
    val weight: BigDecimal,
    val unrealizedPnl: BigDecimal,
) {
    companion object {
        fun from(entity: PortfolioSnapshot): PortfolioSnapshotResponse = PortfolioSnapshotResponse(
            businessDate = entity.businessDate,
            ticker = entity.ticker,
            quantity = entity.quantity,
            avgBuyPrice = entity.avgBuyPrice,
            closePrice = entity.closePrice,
            marketValue = entity.marketValue,
            weight = entity.weight.setScale(6, RoundingMode.HALF_UP),
            unrealizedPnl = entity.unrealizedPnl,
        )
    }
}

data class PortfolioSnapshotJobResponse(
    val accountId: Long,
    val businessDate: LocalDate,
    val snapshotCount: Int,
)