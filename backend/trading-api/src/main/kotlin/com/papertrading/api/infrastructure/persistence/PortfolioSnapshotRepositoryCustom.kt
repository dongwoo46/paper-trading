package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.portfolio.PortfolioSnapshot
import java.time.LocalDate

interface PortfolioSnapshotRepositoryCustom {
    fun searchByAccountIdAndBusinessDate(accountId: Long, businessDate: LocalDate): List<PortfolioSnapshot>
}

