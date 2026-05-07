package com.papertrading.api.application.portfolio.tax

import java.math.BigDecimal
import java.math.RoundingMode

data class TaxComputationResult(
    val totalRealizedPnl: BigDecimal,
    val taxablePnl: BigDecimal,
    val estimatedTax: BigDecimal
) {
    init {
        require(totalRealizedPnl.scale() == 4) { "totalRealizedPnl scale must be 4" }
        require(taxablePnl.scale() == 4) { "taxablePnl scale must be 4" }
        require(estimatedTax.scale() == 4) { "estimatedTax scale must be 4" }
    }

    companion object {
        fun of(totalRealizedPnl: BigDecimal, taxablePnl: BigDecimal, estimatedTax: BigDecimal): TaxComputationResult =
            TaxComputationResult(
                totalRealizedPnl.setScale(4, RoundingMode.HALF_UP),
                taxablePnl.setScale(4, RoundingMode.HALF_UP),
                estimatedTax.setScale(4, RoundingMode.HALF_UP)
            )
    }
}
