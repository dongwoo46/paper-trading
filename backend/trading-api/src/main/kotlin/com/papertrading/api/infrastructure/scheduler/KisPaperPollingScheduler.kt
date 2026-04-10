package com.papertrading.api.infrastructure.scheduler

import com.papertrading.api.application.order.KisPaperOrderExecutor
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class KisPaperPollingScheduler(
    private val kisPaperOrderExecutor: KisPaperOrderExecutor,
) {
    @Scheduled(fixedDelay = 3_000)
    fun pollFills() = kisPaperOrderExecutor.pollFills()
}