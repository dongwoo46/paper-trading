package com.papertrading.api.infrastructure.notification

import com.papertrading.api.application.notification.SlackNotificationRequestedEvent
import mu.KotlinLogging
import org.springframework.http.HttpEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class SlackWebhookNotifier(
    private val restTemplate: RestTemplate,
    private val properties: SlackNotificationProperties,
) : NotificationSender {
    private val log = KotlinLogging.logger {}

    override fun send(event: SlackNotificationRequestedEvent): Boolean {
        if (properties.webhookUrl.isBlank()) return false

        return try {
            restTemplate.postForEntity(
                properties.webhookUrl,
                HttpEntity(mapOf("text" to event.message)),
                String::class.java,
            )
            true
        } catch (ex: Exception) {
            log.warn { "Slack webhook call failed for eventType=${event.eventType}: ${ex.message}" }
            false
        }
    }
}
