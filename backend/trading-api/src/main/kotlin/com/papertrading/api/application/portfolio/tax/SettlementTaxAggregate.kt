package com.papertrading.api.application.portfolio.tax

import java.math.BigDecimal

data class SettlementTaxAggregate(
    val totalRealizedPnl: BigDecimal,
    val totalFee: BigDecimal,
    val totalTax: BigDecimal,
    val currency: String
)
