package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.model.RiskPolicy
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface RiskPolicyRepository : JpaRepository<RiskPolicy, Long> {
    fun findByAccountIdAndIsActiveTrue(accountId: Long): Optional<RiskPolicy>
}
