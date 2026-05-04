package com.papertrading.api.infrastructure.notification

import com.papertrading.api.domain.event.SlackNotificationRequestedEvent

fun interface NotificationSender {
    fun send(event: SlackNotificationRequestedEvent): Boolean
}

