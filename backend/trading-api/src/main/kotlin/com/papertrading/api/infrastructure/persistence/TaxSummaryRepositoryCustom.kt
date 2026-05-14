package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.portfolio.TaxSummary
import java.util.Optional

interface TaxSummaryRepositoryCustom {
    fun findOneByAccountIdAndTaxYear(accountId: Long, taxYear: Int): Optional<TaxSummary>
    fun searchByAccountIdAndTaxYearRange(accountId: Long, fromYear: Int, toYear: Int): List<TaxSummary>
}

