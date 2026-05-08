package com.papertrading.api.application.portfolio.tax

import com.papertrading.api.common.exception.TaxComputationScaleException
import java.math.BigDecimal
import java.math.RoundingMode

data class TaxComputationResult(
    val totalRealizedPnl: BigDecimal,
    val taxablePnl: BigDecimal,
    val estimatedTax: BigDecimal
) {
    init {
        if (totalRealizedPnl.scale() != 4) throw TaxComputationScaleException("totalRealizedPnl")
        if (taxablePnl.scale() != 4) throw TaxComputationScaleException("taxablePnl")
        if (estimatedTax.scale() != 4) throw TaxComputationScaleException("estimatedTax")
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

