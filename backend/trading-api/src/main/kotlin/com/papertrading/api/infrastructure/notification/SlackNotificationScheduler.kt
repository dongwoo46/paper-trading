package com.papertrading.api.infrastructure.notification

import com.papertrading.api.application.notification.port.SlackNotificationConfig
import com.papertrading.api.domain.port.NotificationSender
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SlackNotificationScheduler(
    private val sender: NotificationSender,
    private val policyStore: SlackNotificationConfig,
    private val txService: NotificationPersistenceTxService,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(fixedDelay = 30_000)
    fun retryPending() {
        val policy = policyStore.get()
        if (!policy.enabled) return
        val pendingNotifications = txService.findPendingForRetry(policy.maxRetries)

        for (notification in pendingNotifications) {
            try {
                val claimedNotification = txService.markDelivering(notification)
                if (claimedNotification == null) {
                    log.debug { "Notification id=${notification.id} already claimed by another instance, skipping" }
                    continue
                }

                val success = sender.send(claimedNotification.message)
                if (success) {
                    txService.markDelivered(claimedNotification)
                } else {
                    txService.markFailed(claimedNotification, "Slack webhook returned failure", policy.maxRetries)
                }
            } catch (ex: Exception) {
                log.error(ex) { "Error retrying notification id=${notification.id}, continuing to next record" }
                try {
                    txService.markFailed(notification, ex.message ?: "Unknown error", policy.maxRetries)
                } catch (saveEx: Exception) {
                    log.error(saveEx) { "Failed to update notification after retry error: id=${notification.id}" }
                }
            }
        }
    }
}
