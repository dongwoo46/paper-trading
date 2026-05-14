package com.papertrading.api.application.portfolio.command

import java.math.BigDecimal

data class TaxComputationInput(
    val totalRealizedPnl: BigDecimal,
    val totalFee: BigDecimal,
    val totalTax: BigDecimal,
    val currency: String
)
