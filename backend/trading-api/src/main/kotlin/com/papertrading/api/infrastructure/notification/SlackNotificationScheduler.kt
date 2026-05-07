package com.papertrading.api.infrastructure.notification

import com.papertrading.api.application.notification.SlackNotificationPolicyStore
import com.papertrading.api.application.notification.SlackNotificationRequestedEvent
import com.papertrading.api.domain.enums.DeliveryStatus
import com.papertrading.api.infrastructure.persistence.NotificationRepository
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class SlackNotificationScheduler(
    private val repository: NotificationRepository,
    private val sender: NotificationSender,
    private val policyStore: SlackNotificationPolicyStore,
    private val retryProcessor: NotificationRetryProcessor,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(fixedDelay = 30_000)
    fun retryPending() {
        val policy = policyStore.get()
        val pendingNotifications = repository.findByStatusAndAttemptCountLessThan(
            status = DeliveryStatus.PENDING,
            maxAttempts = policy.maxRetries,
        )

        for (notification in pendingNotifications) {
            try {
                val claimedNotification = retryProcessor.markDelivering(notification)
                if (claimedNotification == null) {
                    log.debug { "Notification id=${notification.id} already claimed by another instance, skipping" }
                    continue
                }

                val event = SlackNotificationRequestedEvent(
                    eventType = claimedNotification.eventType,
                    accountId = 0L,
                    orderId = null,
                    executionId = null,
                    riskEventId = null,
                    message = claimedNotification.message,
                    occurredAt = Instant.now(),
                )

                val success = sender.send(event)
                if (success) {
                    retryProcessor.markDelivered(claimedNotification)
                } else {
                    retryProcessor.markFailed(claimedNotification, "Slack webhook returned failure", policy.maxRetries)
                }
            } catch (ex: Exception) {
                log.error(ex) { "Error retrying notification id=${notification.id}, continuing to next record" }
                try {
                    retryProcessor.markFailed(notification, ex.message ?: "Unknown error", policy.maxRetries)
                } catch (saveEx: Exception) {
                    log.error(saveEx) { "Failed to update notification after retry error: id=${notification.id}" }
                }
            }
        }
    }
}