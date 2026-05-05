package com.papertrading.api.application.notification

import com.papertrading.api.domain.enums.NotificationEventType
import com.papertrading.api.application.notification.SlackNotificationRequestedEvent
import com.papertrading.api.infrastructure.notification.NotificationSender
import com.papertrading.api.infrastructure.notification.SlackNotificationProperties
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

data class SlackNotificationRuntimeConfig(
    val enabled: Boolean,
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

@Component
class SlackNotificationPolicyStore(
    properties: SlackNotificationProperties,
) {
    private val policyRef = AtomicReference(
        SlackNotificationRuntimeConfig(
            enabled = properties.enabled,
            maxRetries = properties.maxRetries,
            retryBackoffMillis = properties.retryBackoffMillis,
            enabledTypes = properties.enabledTypes,
            updatedAt = Instant.now(),
        )
    )

    fun get(): SlackNotificationRuntimeConfig = policyRef.get()

    fun update(newConfig: SlackNotificationRuntimeConfig): SlackNotificationRuntimeConfig {
        policyRef.set(newConfig)
        return newConfig
    }
}

@Service
class SlackNotificationService(
    private val properties: SlackNotificationProperties,
    private val policyStore: SlackNotificationPolicyStore,
    private val sender: NotificationSender,
) : SlackNotificationQueryService, SlackNotificationCommandService {

    override fun getConfig(): SlackNotificationConfigResult = policyStore.get().toResult(properties.webhookUrl.isNotBlank(), properties.timeoutMillis)

    override fun updateConfig(request: UpdateSlackNotificationConfigCommand): SlackNotificationConfigResult {
        require(request.maxRetries >= 1) { "INVALID_RETRY_POLICY" }
        require(request.retryBackoffMillis >= 0) { "INVALID_RETRY_POLICY" }
        if (request.enabled && properties.webhookUrl.isBlank()) {
            throw IllegalStateException("WEBHOOK_NOT_CONFIGURED")
        }
        val updated = policyStore.update(
            SlackNotificationRuntimeConfig(
                enabled = request.enabled,
                maxRetries = request.maxRetries,
                retryBackoffMillis = request.retryBackoffMillis,
                enabledTypes = request.enabledTypes,
                updatedAt = Instant.now(),
            )
        )
        return updated.toResult(properties.webhookUrl.isNotBlank(), properties.timeoutMillis)
    }

    override fun sendTestMessage(message: String): String {
        if (properties.webhookUrl.isBlank()) {
            throw IllegalStateException("WEBHOOK_NOT_CONFIGURED")
        }
        val requestId = UUID.randomUUID().toString()
        val sent = sender.send(
            SlackNotificationRequestedEvent(
                eventType = NotificationEventType.ORDER_ERROR,
                accountId = 0L,
                orderId = null,
                executionId = null,
                riskEventId = null,
                message = message,
                occurredAt = Instant.now(),
            )
        )
        if (!sent) {
            throw SlackWebhookFailedException("SLACK_WEBHOOK_FAILED")
        }
        return requestId
    }

    private fun SlackNotificationRuntimeConfig.toResult(webhookConfigured: Boolean, timeoutMillis: Long): SlackNotificationConfigResult =
        SlackNotificationConfigResult(
            enabled = enabled,
            webhookConfigured = webhookConfigured,
            timeoutMillis = timeoutMillis,
            maxRetries = maxRetries,
            retryBackoffMillis = retryBackoffMillis,
            enabledTypes = enabledTypes,
            updatedAt = updatedAt,
        )
}
