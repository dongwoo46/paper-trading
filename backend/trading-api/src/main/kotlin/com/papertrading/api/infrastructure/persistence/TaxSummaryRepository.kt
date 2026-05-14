package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.portfolio.TaxSummary
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface TaxSummaryRepository : JpaRepository<TaxSummary, Long>, TaxSummaryRepositoryCustom {
    fun findByAccountIdAndTaxYear(accountId: Long, taxYear: Int): Optional<TaxSummary>
    fun findByAccountIdAndTaxYearBetweenOrderByTaxYearDesc(accountId: Long, fromYear: Int, toYear: Int): List<TaxSummary>
}
