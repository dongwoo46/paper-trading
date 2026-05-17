package com.papertrading.api.application.order.result

import com.papertrading.api.domain.entity.order.Execution
import com.papertrading.api.domain.enums.TradingMode
import java.math.BigDecimal
import java.time.Instant

data class ExecutionResult(
    val executionId: Long,
    val orderId: Long,
    val ticker: String,
    val executedQuantity: BigDecimal,
    val executedPrice: BigDecimal,
    val fee: BigDecimal,
    val executedAt: Instant,
    val executionMode: TradingMode,
) {
    companion object {
        fun from(execution: Execution) = ExecutionResult(
            executionId = requireNotNull(execution.id) { "execution.id is null" },
            orderId = requireNotNull(execution.order.id) { "execution.order.id is null" },
            ticker = execution.ticker,
            executedQuantity = execution.executedQuantity,
            executedPrice = execution.executedPrice,
            fee = execution.fee,
            executedAt = requireNotNull(execution.executedAt) { "execution.executedAt is null" },
            executionMode = execution.executionMode,
        )
    }
}
