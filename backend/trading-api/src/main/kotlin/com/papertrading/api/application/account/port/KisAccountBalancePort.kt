package com.papertrading.api.application.account.port

import java.math.BigDecimal
import java.time.OffsetDateTime

interface KisAccountBalancePort {
    fun fetchBalance(accountId: Long, trId: String): KisBalanceSnapshot
}

data class KisBalanceSnapshot(
    val asOf: OffsetDateTime,
    val cashBalance: BigDecimal,
    val marketValue: BigDecimal,
    val unrealizedPnl: BigDecimal,
    val returnRate: BigDecimal,
    val positions: List<KisBalancePosition>
)

data class KisBalancePosition(
    val ticker: String,
    val quantity: BigDecimal,
    val avgPrice: BigDecimal,
    val currentPrice: BigDecimal,
    val marketValue: BigDecimal,
    val unrealizedPnl: BigDecimal,
    val returnRate: BigDecimal
)
