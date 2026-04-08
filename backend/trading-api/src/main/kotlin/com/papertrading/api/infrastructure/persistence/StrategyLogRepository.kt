package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.model.StrategyLog
import org.springframework.data.jpa.repository.JpaRepository

interface StrategyLogRepository : JpaRepository<StrategyLog, Long> {
    fun findByStrategyIdOrderByLoggedAtDesc(strategyId: Long): List<StrategyLog>
    fun findByStrategyIdAndLogLevelOrderByLoggedAtDesc(strategyId: Long, logLevel: String): List<StrategyLog>
}
