package com.papertrading.api.application.portfolio.tax

import java.time.Instant

interface SettlementTaxReadRepository {
    fun summarizeForTax(accountId: Long, yearStart: Instant, yearEnd: Instant): SettlementTaxAggregate
}
