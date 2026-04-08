package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.model.Execution
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface ExecutionRepository : JpaRepository<Execution, Long> {
    fun findByExternalExecutionId(externalExecutionId: String): Optional<Execution>
    fun findByOrderId(orderId: Long): List<Execution>
    fun findByAccountIdOrderByExecutedAtDesc(accountId: Long): List<Execution>
}
