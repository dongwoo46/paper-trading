package com.papertrading.api.application.notification.port

import com.papertrading.api.domain.enums.NotificationEventType
import java.time.Instant

data class SlackNotificationRuntimeConfig(
    val enabled: Boolean,
    val webhookConfigured: Boolean,
    val timeoutMillis: Long,
    val maxRetries: Int,
    val retryBackoffMillis: Long,
    val enabledTypes: List<NotificationEventType>,
    val updatedAt: Instant,
)
