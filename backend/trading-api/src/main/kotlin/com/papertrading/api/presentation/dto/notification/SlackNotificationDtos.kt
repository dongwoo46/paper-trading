package com.papertrading.api.presentation.dto.notification

import com.papertrading.api.application.notification.result.SlackNotificationConfigResult
import com.papertrading.api.application.notification.command.UpdateSlackNotificationConfigCommand
import com.papertrading.api.domain.enums.NotificationEventType
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
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
    @field:Min(1)
    val maxRetries: Int,
    @field:Min(0)
    val retryBackoffMillis: Long,
    @field:NotEmpty
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
    @field:NotBlank
    val message: String,
)

data class SlackNotificationTestResponse(
    val accepted: Boolean,
    val requestId: String,
)
