package com.papertrading.api.infrastructure.notification

import com.papertrading.api.domain.port.NotificationSender
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

    override fun send(message: String): Boolean {
        if (properties.webhookUrl.isBlank()) return false

        return try {
            restTemplate.postForEntity(
                properties.webhookUrl,
                HttpEntity(mapOf("text" to message)),
                String::class.java,
            )
            true
        } catch (ex: Exception) {
            log.warn { "Slack webhook call failed: ${ex.message}" }
            false
        }
    }
}
