package com.papertrading.api.infrastructure.scheduler

import com.papertrading.api.application.order.KisPaperOrderExecutor

// 스케쥴러 삭제 필요
class KisPaperPollingScheduler(
    private val kisPaperOrderExecutor: KisPaperOrderExecutor,
) {
    fun pollFills() = kisPaperOrderExecutor.pollFills()
}
