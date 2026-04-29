package com.papertrading.collector.application.kis.dto

import com.papertrading.collector.domain.kis.WsConnectionStatus
import java.time.Instant

data class WsHealthSnapshot(
    val mode: String,
    val status: WsConnectionStatus,
    val lastConnectedAt: Instant?,
    val reconnectAttempts: Long,
)
