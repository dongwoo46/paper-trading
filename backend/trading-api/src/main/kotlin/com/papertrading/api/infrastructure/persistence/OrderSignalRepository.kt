package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.model.OrderSignal
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface OrderSignalRepository : JpaRepository<OrderSignal, Long> {
    fun findByIdempotencyKey(idempotencyKey: String): Optional<OrderSignal>
    fun findByAccountIdAndIsProcessedFalseOrderByCreatedAtAsc(accountId: Long): List<OrderSignal>
}
