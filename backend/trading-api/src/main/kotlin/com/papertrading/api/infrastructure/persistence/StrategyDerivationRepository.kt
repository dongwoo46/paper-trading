package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.model.StrategyDerivation
import org.springframework.data.jpa.repository.JpaRepository

interface StrategyDerivationRepository : JpaRepository<StrategyDerivation, Long> {
    fun findByParentStrategyId(parentStrategyId: Long): List<StrategyDerivation>
    fun findByChildStrategyId(childStrategyId: Long): List<StrategyDerivation>
}
