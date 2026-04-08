package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.model.PortfolioSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.Optional

interface PortfolioSnapshotRepository : JpaRepository<PortfolioSnapshot, Long> {
    fun findByAccountIdAndSnapshotDate(accountId: Long, date: LocalDate): Optional<PortfolioSnapshot>
    fun findByAccountIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
        accountId: Long,
        from: LocalDate,
        to: LocalDate
    ): List<PortfolioSnapshot>
}
