package com.papertrading.api.infrastructure.sse

import com.papertrading.api.domain.event.ExecutionFilledEvent
import com.papertrading.api.presentation.dto.sse.ExecutionSseEvent
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ExecutionSseEventHandler(
    private val registry: ExecutionSseRegistry,
) {
    private val log = KotlinLogging.logger {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    fun handleExecutionFilled(event: ExecutionFilledEvent) {
        val sseEvent = ExecutionSseEvent.from(event)
        log.info { "Broadcasting SSE execution event: executionId=${event.executionId}, ticker=${event.ticker}" }
        registry.broadcast(sseEvent)
    }
}
