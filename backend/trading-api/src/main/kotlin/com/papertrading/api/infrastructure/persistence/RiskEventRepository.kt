package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.model.RiskEvent
import org.springframework.data.jpa.repository.JpaRepository

interface RiskEventRepository : JpaRepository<RiskEvent, Long> {
    fun findByAccountIdOrderByTriggeredAtDesc(accountId: Long): List<RiskEvent>
}
