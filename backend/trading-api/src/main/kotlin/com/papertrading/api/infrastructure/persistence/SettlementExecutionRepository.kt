package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.model.SettlementExecution
import org.springframework.data.jpa.repository.JpaRepository

interface SettlementExecutionRepository : JpaRepository<SettlementExecution, Long> {
    fun findBySettlementId(settlementId: Long): List<SettlementExecution>
}
