package com.papertrading.api.application.notification.result

import com.papertrading.api.application.notification.port.SlackNotificationRuntimeConfig
import com.papertrading.api.domain.enums.NotificationEventType
import java.time.Instant

data class SlackNotificationConfigResult(
    val enabled: Boolean,
    val webhookConfigured: Boolean,
    val timeoutMillis: Long,
    val maxRetries: Int,
    val retryBackoffMillis: Long,
    val enabledTypes: List<NotificationEventType>,
    val updatedAt: Instant,
) {
    companion object {
        fun from(config: SlackNotificationRuntimeConfig): SlackNotificationConfigResult =
            SlackNotificationConfigResult(
                enabled = config.enabled,
                webhookConfigured = config.webhookConfigured,
                timeoutMillis = config.timeoutMillis,
                maxRetries = config.maxRetries,
                retryBackoffMillis = config.retryBackoffMillis,
                enabledTypes = config.enabledTypes,
                updatedAt = config.updatedAt,
            )
    }
}
