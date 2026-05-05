package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.strategy.StrategyVersion
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface StrategyVersionRepository : JpaRepository<StrategyVersion, Long> {
    fun findByStrategyIdOrderByVersionNoDesc(strategyId: Long): List<StrategyVersion>
    fun findTopByStrategyIdOrderByVersionNoDesc(strategyId: Long): Optional<StrategyVersion>
}
