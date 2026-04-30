package com.papertrading.collector.presentation.subscriptions

import com.papertrading.collector.application.subscriptions.dto.SubscriptionModeStatus
import com.papertrading.collector.application.subscriptions.dto.SubscriptionStatusReport
import com.papertrading.collector.application.subscriptions.service.SubscriptionStatusService
import com.papertrading.collector.presentation.subscriptions.dto.SubscriptionModeStatusResponse
import com.papertrading.collector.presentation.subscriptions.dto.SubscriptionStatusResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/subscriptions")
class SubscriptionStatusController(
    private val statusService: SubscriptionStatusService,
) {
    @GetMapping("/status")
    fun status(): SubscriptionStatusResponse {
        return statusService.report().toResponse()
    }

    private fun SubscriptionStatusReport.toResponse() = SubscriptionStatusResponse(
        generatedAt = generatedAt.toString(),
        totalWsSlotUsed = totalWsSlotUsed,
        totalWsSlotMax = totalWsSlotMax,
        modes = modes.map { it.toResponse() },
    )

    private fun SubscriptionModeStatus.toResponse() = SubscriptionModeStatusResponse(
        mode = mode,
        connectionStatus = connectionStatus.name,
        lastConnectedAt = lastConnectedAt?.toString(),
        reconnectAttempts = reconnectAttempts,
        wsSymbols = wsSymbols,
        restSymbols = restSymbols,
        wsSlotUsed = wsSlotUsed,
        wsSlotMax = wsSlotMax,
    )
}
