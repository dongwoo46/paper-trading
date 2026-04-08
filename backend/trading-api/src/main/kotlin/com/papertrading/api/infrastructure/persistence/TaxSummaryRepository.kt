package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.model.TaxSummary
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface TaxSummaryRepository : JpaRepository<TaxSummary, Long> {
    fun findByAccountIdAndTaxYear(accountId: Long, taxYear: Int): Optional<TaxSummary>
}
