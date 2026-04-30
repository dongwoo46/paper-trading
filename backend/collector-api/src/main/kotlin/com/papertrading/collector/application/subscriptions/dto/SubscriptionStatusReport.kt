package com.papertrading.collector.application.subscriptions.dto

import java.time.Instant

data class SubscriptionStatusReport(
    val generatedAt: Instant,
    val modes: List<SubscriptionModeStatus>,
    val totalWsSlotUsed: Int,
    val totalWsSlotMax: Int,
)
