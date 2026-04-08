package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.model.Settlement
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface SettlementRepository : JpaRepository<Settlement, Long> {
    fun findByOrderId(orderId: Long): Optional<Settlement>
    fun findByAccountIdOrderBySettledAtDesc(accountId: Long): List<Settlement>
}
