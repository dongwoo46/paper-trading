package com.papertrading.api.application.order

import com.papertrading.api.application.order.query.ExecutionQuery
import com.papertrading.api.application.order.query.OrderListQuery
import com.papertrading.api.application.order.result.ExecutionResult
import com.papertrading.api.application.order.result.OrderResult
import com.papertrading.api.common.exception.OrderNotFoundException
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
    fun getOrder(accountId: Long, orderId: Long): OrderResult {
        val order = orderRepository.findByIdAndAccountId(orderId, accountId)
            .orElseThrow { OrderNotFoundException(orderId) }
        return OrderResult.from(order)
    }

    fun listOrders(query: OrderListQuery): List<OrderResult> {
        validateOrderRange(query)
        return orderRepository.searchOrders(query)
            .map { OrderResult.from(it) }
    }

    fun getExecution(accountId: Long, orderId: Long, executionId: Long): ExecutionResult {
        ensureOrderOwnership(accountId, orderId)
        return executionRepository.findByIdAndOrderIdAndAccountId(executionId, orderId, accountId)
            .map { ExecutionResult.from(it) }
            .orElseThrow { NoSuchElementException("execution not found: executionId=$executionId") }
    }

    fun listExecutions(query: ExecutionQuery): List<ExecutionResult> {
        ensureOrderOwnership(query.accountId, query.orderId)
        validateExecutionRange(query)
        return executionRepository.searchExecutions(query).map { ExecutionResult.from(it) }
    }

    private fun ensureOrderOwnership(accountId: Long, orderId: Long) {
        orderRepository.findByIdAndAccountId(orderId, accountId)
            .orElseThrow { OrderNotFoundException(orderId) }
    }

    private fun validateExecutionRange(query: ExecutionQuery) {
        if (query.executedFrom != null && query.executedTo != null) {
            require(!query.executedFrom.isAfter(query.executedTo)) {
                "executedFrom must be before or equal to executedTo"
            }
        }
    }

    private fun validateOrderRange(query: OrderListQuery) {
        if (query.createdFrom != null && query.createdTo != null) {
            require(!query.createdFrom.isAfter(query.createdTo)) {
                "createdFrom must be before or equal to createdTo"
            }
        }
    }
}
