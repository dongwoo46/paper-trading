package com.papertrading.api.application.notification

import com.papertrading.api.domain.enums.NotificationEventType
import java.time.Instant

data class SlackNotificationRequestedEvent(
    val eventType: NotificationEventType,
    val accountId: Long,
    val orderId: Long?,
    val executionId: Long?,
    val riskEventId: Long?,
    val message: String,
    val occurredAt: Instant,
)