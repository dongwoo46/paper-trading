package com.papertrading.api.infrastructure.notification

import com.papertrading.api.application.notification.SlackNotificationPolicy
import com.papertrading.api.application.notification.SlackNotificationRuntimeConfig
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

@Component
class SlackNotificationPolicyStore(
    properties: SlackNotificationProperties,
) : SlackNotificationPolicy {

    private val policyRef = AtomicReference(
        SlackNotificationRuntimeConfig(
            enabled = properties.enabled,
            webhookConfigured = properties.webhookUrl.isNotBlank(),
            timeoutMillis = properties.timeoutMillis,
            maxRetries = properties.maxRetries,
            retryBackoffMillis = properties.retryBackoffMillis,
            enabledTypes = properties.enabledTypes,
            updatedAt = Instant.now(),
        )
    )

    override fun get(): SlackNotificationRuntimeConfig = policyRef.get()

    override fun update(newConfig: SlackNotificationRuntimeConfig): SlackNotificationRuntimeConfig {
        policyRef.set(newConfig)
        return newConfig
    }
}