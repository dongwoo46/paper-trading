package com.papertrading.api.application.account.result

import java.math.BigDecimal

data class KisBalancePositionResult(
    val ticker: String,
    val quantity: BigDecimal,
    val avgPrice: BigDecimal,
    val currentPrice: BigDecimal,
    val marketValue: BigDecimal,
    val unrealizedPnl: BigDecimal,
    val returnRate: BigDecimal
)
