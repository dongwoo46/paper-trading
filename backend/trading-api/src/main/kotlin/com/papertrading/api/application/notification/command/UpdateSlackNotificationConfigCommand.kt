package com.papertrading.api.application.notification.command

import com.papertrading.api.domain.enums.NotificationEventType

data class UpdateSlackNotificationConfigCommand(
    val enabled: Boolean,
    val maxRetries: Int,
    val retryBackoffMillis: Long,
    val enabledTypes: List<NotificationEventType>,
)
