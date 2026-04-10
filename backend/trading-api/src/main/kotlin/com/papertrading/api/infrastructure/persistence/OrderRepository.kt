package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.model.Order
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface OrderRepository : JpaRepository<Order, Long>, OrderRepositoryCustom {
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    fun findByIdWithOptimisticLock(@Param("id") id: Long): Optional<Order>

    fun findByAccountIdAndOrderStatusIn(accountId: Long, statuses: List<OrderStatus>): List<Order>
    fun existsByAccountIdAndIdempotencyKey(accountId: Long, idempotencyKey: String): Boolean
    fun findByAccountIdAndIdempotencyKey(accountId: Long, idempotencyKey: String): Order?
    fun findByAccountIdOrderByCreatedAtDesc(accountId: Long): List<Order>
}
