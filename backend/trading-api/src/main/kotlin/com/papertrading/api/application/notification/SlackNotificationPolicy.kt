package com.papertrading.api.application.notification

interface SlackNotificationPolicy {
    fun get(): SlackNotificationRuntimeConfig
    fun update(newConfig: SlackNotificationRuntimeConfig): SlackNotificationRuntimeConfig
}