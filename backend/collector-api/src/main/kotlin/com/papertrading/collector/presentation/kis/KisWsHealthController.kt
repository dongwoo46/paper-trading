package com.papertrading.collector.presentation.kis

import com.papertrading.collector.application.kis.dto.WsHealthSnapshot
import com.papertrading.collector.application.kis.service.KisWsHealthService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class WsHealthSnapshotResponse(
    val mode: String,
    val status: String,
    val lastConnectedAt: String?,
    val reconnectAttempts: Long,
)

@RestController
@RequestMapping("/api/kis/ws")
class KisWsHealthController(
    private val healthService: KisWsHealthService,
) {
    @GetMapping("/health")
    fun health(): List<WsHealthSnapshotResponse> {
        return healthService.health().map { it.toResponse() }
    }

    private fun WsHealthSnapshot.toResponse() = WsHealthSnapshotResponse(
        mode = mode,
        status = status.name,
        lastConnectedAt = lastConnectedAt?.toString(),
        reconnectAttempts = reconnectAttempts,
    )
}
