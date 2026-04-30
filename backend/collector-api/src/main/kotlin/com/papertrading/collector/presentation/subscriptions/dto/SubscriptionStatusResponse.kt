package com.papertrading.collector.presentation.subscriptions.dto

data class SubscriptionModeStatusResponse(
    val mode: String,
    val connectionStatus: String,
    val lastConnectedAt: String?,
    val reconnectAttempts: Long,
    val wsSymbols: List<String>,
    val restSymbols: List<String>,
    val wsSlotUsed: Int,
    val wsSlotMax: Int,
)

data class SubscriptionStatusResponse(
    val generatedAt: String,
    val totalWsSlotUsed: Int,
    val totalWsSlotMax: Int,
    val modes: List<SubscriptionModeStatusResponse>,
)
