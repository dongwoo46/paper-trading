package com.papertrading.api.presentation.dto.notification

import com.papertrading.api.application.notification.SlackNotificationConfigResult
import com.papertrading.api.application.notification.UpdateSlackNotificationConfigCommand
import com.papertrading.api.domain.enums.NotificationEventType
import java.time.Instant

data class SlackNotificationConfigResponse(
    val enabled: Boolean,
    val webhookConfigured: Boolean,
    val timeoutMillis: Long,
    val maxRetries: Int,
    val retryBackoffMillis: Long,
    val enabledTypes: List<NotificationEventType>,
    val updatedAt: Instant,
) {
    companion object {
        fun from(result: SlackNotificationConfigResult): SlackNotificationConfigResponse =
            SlackNotificationConfigResponse(
                enabled = result.enabled,
                webhookConfigured = result.webhookConfigured,
                timeoutMillis = result.timeoutMillis,
                maxRetries = result.maxRetries,
                retryBackoffMillis = result.retryBackoffMillis,
                enabledTypes = result.enabledTypes,
                updatedAt = result.updatedAt,
            )
    }
}

data class UpdateSlackNotificationConfigRequest(
    val enabled: Boolean,
    val maxRetries: Int,
    val retryBackoffMillis: Long,
    val enabledTypes: List<NotificationEventType>,
) {
    fun toCommand(): UpdateSlackNotificationConfigCommand =
        UpdateSlackNotificationConfigCommand(
            enabled = enabled,
            maxRetries = maxRetries,
            retryBackoffMillis = retryBackoffMillis,
            enabledTypes = enabledTypes,
        )
}

data class SlackNotificationTestRequest(
    val message: String,
)

data class SlackNotificationTestResponse(
    val accepted: Boolean,
    val requestId: String,
)
