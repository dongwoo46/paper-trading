package com.papertrading.api.presentation.dto.sse

import com.papertrading.api.domain.event.ExecutionFilledEvent
import java.time.format.DateTimeFormatter

data class ExecutionSseEvent(
    val executionId: Long,
    val orderId: Long,
    val ticker: String,
    val tickerName: String?,
    val side: String,
    val quantity: String,
    val price: String,
    val fee: String,
    val currency: String,
    val executedAt: String,
) {
    companion object {
        fun from(e: ExecutionFilledEvent): ExecutionSseEvent = ExecutionSseEvent(
            executionId = e.executionId,
            orderId = e.orderId,
            ticker = e.ticker,
            tickerName = null,
            side = e.side.name,
            quantity = e.quantity.toPlainString(),
            price = e.price.toPlainString(),
            fee = e.fee.toPlainString(),
            currency = e.currency,
            executedAt = DateTimeFormatter.ISO_INSTANT.format(e.executedAt),
        )
    }
}
