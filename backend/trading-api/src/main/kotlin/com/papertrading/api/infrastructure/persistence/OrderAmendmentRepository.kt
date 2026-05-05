package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.order.OrderAmendment
import org.springframework.data.jpa.repository.JpaRepository

interface OrderAmendmentRepository : JpaRepository<OrderAmendment, Long> {
    fun findByOrderIdOrderByCreatedAtAsc(orderId: Long): List<OrderAmendment>
}
