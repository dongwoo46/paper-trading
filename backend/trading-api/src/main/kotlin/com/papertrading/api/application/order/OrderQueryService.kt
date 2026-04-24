package com.papertrading.api.application.order

import com.papertrading.api.domain.model.Execution
import com.papertrading.api.domain.model.Order
import com.papertrading.api.infrastructure.persistence.ExecutionRepository
import com.papertrading.api.infrastructure.persistence.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OrderQueryService(
    private val orderRepository: OrderRepository,
    private val executionRepository: ExecutionRepository,
) {
    fun getOrder(accountId: Long, orderId: Long): Order {
        val order = orderRepository.findById(orderId)
            .orElseThrow { NoSuchElementException("주문을 찾을 수 없습니다. orderId=$orderId") }
        check(order.account?.id == accountId) { "해당 계좌의 주문이 아닙니다." }
        return order
    }

    fun listOrders(accountId: Long): List<Order> =
        orderRepository.findByAccountIdOrderByCreatedAtDesc(accountId)

    fun listExecutions(accountId: Long, orderId: Long): List<Execution> {
        getOrder(accountId, orderId) // 권한 검증
        return executionRepository.findByOrderId(orderId)
    }
}
