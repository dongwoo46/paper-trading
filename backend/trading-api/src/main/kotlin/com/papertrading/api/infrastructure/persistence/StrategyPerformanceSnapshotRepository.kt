package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.model.StrategyPerformanceSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface StrategyPerformanceSnapshotRepository : JpaRepository<StrategyPerformanceSnapshot, Long> {
    fun findByStrategyIdAndPeriodStartGreaterThanEqualOrderByPeriodStartAsc(
        strategyId: Long,
        from: LocalDate
    ): List<StrategyPerformanceSnapshot>
}
