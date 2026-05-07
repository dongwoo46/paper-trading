package com.papertrading.api.application.notification

import com.papertrading.api.domain.enums.NotificationEventType
import com.papertrading.api.domain.port.NotificationSender
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

data class SlackNotificationRuntimeConfig(
    val enabled: Boolean,
    val webhookConfigured: Boolean,
    val timeoutMillis: Long,
    val maxRetries: Int,
    val retryBackoffMillis: Long,
    val enabledTypes: List<NotificationEventType>,
    val updatedAt: Instant,
)

data class SlackNotificationConfigResult(
    val enabled: Boolean,
    val webhookConfigured: Boolean,
    val timeoutMillis: Long,
    val maxRetries: Int,
    val retryBackoffMillis: Long,
    val enabledTypes: List<NotificationEventType>,
    val updatedAt: Instant,
)

data class UpdateSlackNotificationConfigCommand(
    val enabled: Boolean,
    val maxRetries: Int,
    val retryBackoffMillis: Long,
    val enabledTypes: List<NotificationEventType>,
)

interface SlackNotificationQueryService {
    fun getConfig(): SlackNotificationConfigResult
}

interface SlackNotificationCommandService {
    fun updateConfig(request: UpdateSlackNotificationConfigCommand): SlackNotificationConfigResult
    fun sendTestMessage(message: String): String
}

class SlackWebhookFailedException(message: String) : RuntimeException(message)

@Service
class SlackNotificationService(
    private val policyStore: SlackNotificationPolicy,
    private val sender: NotificationSender,
) : SlackNotificationQueryService, SlackNotificationCommandService {

    override fun getConfig(): SlackNotificationConfigResult =
        policyStore.get().toResult()

    override fun updateConfig(request: UpdateSlackNotificationConfigCommand): SlackNotificationConfigResult {
        require(request.maxRetries >= 1) { "INVALID_RETRY_POLICY" }
        require(request.retryBackoffMillis >= 0) { "INVALID_RETRY_POLICY" }
        val current = policyStore.get()
        if (request.enabled && !current.webhookConfigured) {
            throw IllegalStateException("WEBHOOK_NOT_CONFIGURED")
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
        return updated.toResult()
    }

    override fun sendTestMessage(message: String): String {
        if (!policyStore.get().webhookConfigured) {
            throw IllegalStateException("WEBHOOK_NOT_CONFIGURED")
        }
        val requestId = UUID.randomUUID().toString()
        val sent = sender.send(message)
        if (!sent) throw SlackWebhookFailedException("SLACK_WEBHOOK_FAILED")
        return requestId
    }

    private fun SlackNotificationRuntimeConfig.toResult() = SlackNotificationConfigResult(
        enabled = enabled,
        webhookConfigured = webhookConfigured,
        timeoutMillis = timeoutMillis,
        maxRetries = maxRetries,
        retryBackoffMillis = retryBackoffMillis,
        enabledTypes = enabledTypes,
        updatedAt = updatedAt,
    )
}