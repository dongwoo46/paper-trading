package com.papertrading.collector.application.subscriptions.dto

import com.papertrading.collector.domain.kis.WsConnectionStatus
import java.time.Instant

data class SubscriptionModeStatus(
    val mode: String,
    val connectionStatus: WsConnectionStatus,
    val lastConnectedAt: Instant?,
    val reconnectAttempts: Long,
    val wsSymbols: List<String>,
    val restSymbols: List<String>,
    val wsSlotUsed: Int,
    val wsSlotMax: Int,
)
