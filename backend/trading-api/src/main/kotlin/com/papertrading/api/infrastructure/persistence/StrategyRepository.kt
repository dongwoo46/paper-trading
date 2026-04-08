package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.enums.ApprovalStatus
import com.papertrading.api.domain.model.Strategy
import org.springframework.data.jpa.repository.JpaRepository

interface StrategyRepository : JpaRepository<Strategy, Long> {
    fun findByAccountIdAndIsActiveTrueAndApprovalStatus(accountId: Long, approvalStatus: ApprovalStatus): List<Strategy>
}
