package com.papertrading.api.application.notification.port

interface SlackNotificationConfig {
    fun get(): SlackNotificationRuntimeConfig
    fun update(newConfig: SlackNotificationRuntimeConfig): SlackNotificationRuntimeConfig
}
