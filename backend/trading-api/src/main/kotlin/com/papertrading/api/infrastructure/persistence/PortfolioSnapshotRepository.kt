package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.portfolio.PortfolioSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.Optional

interface PortfolioSnapshotRepository : JpaRepository<PortfolioSnapshot, Long>, PortfolioSnapshotRepositoryCustom {
    fun findByAccountIdAndBusinessDateAndTicker(
        accountId: Long,
        businessDate: LocalDate,
        ticker: String
    ): Optional<PortfolioSnapshot>

    fun findByAccountIdAndBusinessDateOrderByTickerAsc(
        accountId: Long,
        businessDate: LocalDate
    ): List<PortfolioSnapshot>

    fun findByAccountIdAndBusinessDateBetweenOrderByBusinessDateAsc(
        accountId: Long,
        from: LocalDate,
        to: LocalDate
    ): List<PortfolioSnapshot>
}
