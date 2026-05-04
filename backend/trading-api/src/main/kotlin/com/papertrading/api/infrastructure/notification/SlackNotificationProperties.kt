package com.papertrading.api.infrastructure.notification

import com.papertrading.api.domain.enums.NotificationEventType
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "trading.notification.slack")
data class SlackNotificationProperties(
    val enabled: Boolean = false,
    val webhookUrl: String = "",
    val timeoutMillis: Long = 3000L,
    val maxRetries: Int = 1,
    val retryBackoffMillis: Long = 500L,
    val enabledTypes: List<NotificationEventType> = emptyList(),
)

