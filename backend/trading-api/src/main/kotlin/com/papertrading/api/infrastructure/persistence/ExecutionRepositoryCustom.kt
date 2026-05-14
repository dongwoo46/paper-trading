package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.application.order.query.ExecutionQuery
import com.papertrading.api.domain.entity.order.Execution
import java.util.Optional

interface ExecutionRepositoryCustom {
    fun searchExecutions(query: ExecutionQuery): List<Execution>
    fun findByIdAndOrderIdAndAccountId(executionId: Long, orderId: Long, accountId: Long): Optional<Execution>
}

