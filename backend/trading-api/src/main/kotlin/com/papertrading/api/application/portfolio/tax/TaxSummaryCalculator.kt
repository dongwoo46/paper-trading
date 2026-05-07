package com.papertrading.api.application.portfolio.tax

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

@Component
class TaxSummaryCalculator {

    fun compute(input: SettlementTaxAggregate): TaxComputationResult {
        require(input.currency == "KRW") { "지원하지 않는 통화입니다. currency=${input.currency}" }

        val totalRealized = input.totalRealizedPnl.setScale(4, RoundingMode.HALF_UP)
        val taxable = totalRealized.subtract(input.totalFee).subtract(input.totalTax).setScale(4, RoundingMode.HALF_UP)
        val taxableNonNegative = taxable.max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)
        val estimated = taxableNonNegative.multiply(TAX_RATE).setScale(4, RoundingMode.HALF_UP)

        return TaxComputationResult.of(
            totalRealizedPnl = totalRealized,
            taxablePnl = taxableNonNegative,
            estimatedTax = estimated
        )
    }

    companion object {
        private val TAX_RATE = BigDecimal("0.2200")
    }
}
