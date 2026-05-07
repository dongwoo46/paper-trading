package com.papertrading.api.presentation.dto.portfolio

import com.papertrading.api.domain.entity.portfolio.TaxSummary
import com.papertrading.api.domain.enums.TaxSummaryStatus
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant

data class TaxSummaryResponse(
    val id: Long,
    val accountId: Long,
    val taxYear: Int,
    val totalRealizedPnl: BigDecimal,
    val taxablePnl: BigDecimal,
    val estimatedTax: BigDecimal,
    val status: TaxSummaryStatus,
    val computedAt: Instant,
) {
    companion object {
        fun from(summary: TaxSummary): TaxSummaryResponse = TaxSummaryResponse(
            id = summary.id ?: 0L,
            accountId = summary.account.id ?: 0L,
            taxYear = summary.taxYear,
            totalRealizedPnl = summary.totalRealizedPnl,
            taxablePnl = summary.taxablePnl,
            estimatedTax = summary.estimatedTax,
            status = summary.status,
            computedAt = summary.computedAt,
        )
    }
}

data class RecalculateTaxSummaryRequest(
    val force: Boolean = false,
)

data class YearEndBatchRequest(
    @field:NotNull
    val taxYear: Int?,
    val accountIds: List<Long>? = null,
)

data class YearEndBatchResponse(
    val taxYear: Int,
    val requestedAccounts: Int,
)
