package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.model.StrategyVersion
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface StrategyVersionRepository : JpaRepository<StrategyVersion, Long> {
    fun findByStrategyIdOrderByVersionNoDesc(strategyId: Long): List<StrategyVersion>
    fun findTopByStrategyIdOrderByVersionNoDesc(strategyId: Long): Optional<StrategyVersion>
}
