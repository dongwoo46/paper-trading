package com.papertrading.api.infrastructure.notification

import com.papertrading.api.application.notification.SlackNotificationRequestedEvent

fun interface NotificationSender {
    fun send(event: SlackNotificationRequestedEvent): Boolean
}

