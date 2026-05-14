package com.papertrading.api.application.portfolio.result

import java.math.BigDecimal
import java.math.RoundingMode

data class TaxComputationResult private constructor(
    val totalRealizedPnl: BigDecimal,
    val taxablePnl: BigDecimal,
    val estimatedTax: BigDecimal,
) {
    companion object {
        fun of(
            totalRealizedPnl: BigDecimal,
            taxablePnl: BigDecimal,
            estimatedTax: BigDecimal,
        ): TaxComputationResult {
            return TaxComputationResult(
                totalRealizedPnl.setScale(4, RoundingMode.HALF_UP),
                taxablePnl.setScale(4, RoundingMode.HALF_UP),
                estimatedTax.setScale(4, RoundingMode.HALF_UP),
            )
        }
    }
}
