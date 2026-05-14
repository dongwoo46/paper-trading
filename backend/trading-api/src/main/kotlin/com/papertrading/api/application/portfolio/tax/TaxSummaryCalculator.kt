package com.papertrading.api.application.portfolio.tax

import com.papertrading.api.application.portfolio.result.TaxComputationResult
import com.papertrading.api.common.exception.UnsupportedCurrencyException
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

@Component
class TaxSummaryCalculator {
    fun compute(input: TaxSettlementAggregate): TaxComputationResult {
        if (input.currency != "KRW") {
            throw UnsupportedCurrencyException("지원하지 않는 통화입니다. currency=${input.currency}")
        }

        val totalRealized = input.totalRealizedPnl.setScale(4, RoundingMode.HALF_UP)
        val taxable = totalRealized.subtract(input.totalFee).subtract(input.totalTax).setScale(4, RoundingMode.HALF_UP)
        val taxableNonNegative = taxable.max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)
        val estimated = input.totalTax.max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)

        return TaxComputationResult.of(
            totalRealizedPnl = totalRealized,
            taxablePnl = taxableNonNegative,
            estimatedTax = estimated
        )
    }
}
