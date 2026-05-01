package com.papertrading.api.infrastructure.scheduler

import com.papertrading.api.application.order.KisPaperOrderExecutor

class KisPaperPollingScheduler(
    private val kisPaperOrderExecutor: KisPaperOrderExecutor,
) {
    fun pollFills() = kisPaperOrderExecutor.pollFills()
}
