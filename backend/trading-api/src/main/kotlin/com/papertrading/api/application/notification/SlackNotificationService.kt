package com.papertrading.api.application.notification

import com.papertrading.api.application.notification.command.UpdateSlackNotificationConfigCommand
import com.papertrading.api.application.notification.port.SlackNotificationConfig
import com.papertrading.api.application.notification.result.SlackNotificationConfigResult
import com.papertrading.api.domain.enums.NotificationEventType
import com.papertrading.api.domain.port.NotificationSender
import com.papertrading.api.common.exception.ApiDomainException
import com.papertrading.api.common.exception.InvalidRetryPolicyException
import com.papertrading.api.common.exception.SlackWebhookFailedException
import com.papertrading.api.common.exception.WebhookNotConfiguredException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class SlackNotificationService(
    private val policyStore: SlackNotificationConfig,
    private val sender: NotificationSender,
)  {

    fun getConfig(): SlackNotificationConfigResult =
        SlackNotificationConfigResult.from(policyStore.get())

    fun updateConfig(request: UpdateSlackNotificationConfigCommand): SlackNotificationConfigResult {
        if (request.maxRetries < 1 || request.retryBackoffMillis < 0) {
            throw InvalidRetryPolicyException()
        }
        val current = policyStore.get()
        if (request.enabled && !current.webhookConfigured) {
            throw WebhookNotConfiguredException()
        }
        val updated = policyStore.update(
            current.copy(
                enabled = request.enabled,
                maxRetries = request.maxRetries,
                retryBackoffMillis = request.retryBackoffMillis,
                enabledTypes = request.enabledTypes,
                updatedAt = Instant.now(),
            )
        )
        return SlackNotificationConfigResult.from(updated)
    }

    fun sendTestMessage(message: String): String {
        if (!policyStore.get().webhookConfigured) {
            throw WebhookNotConfiguredException()
        }
        val requestId = UUID.randomUUID().toString()
        val sent = sender.send(message)
        if (!sent) throw SlackWebhookFailedException("SLACK_WEBHOOK_FAILED")
        return requestId
    }
}
