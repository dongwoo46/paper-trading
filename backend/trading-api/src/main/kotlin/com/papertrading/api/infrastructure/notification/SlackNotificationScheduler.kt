package com.papertrading.api.infrastructure.notification

import com.papertrading.api.application.notification.SlackNotificationPolicyStore
import com.papertrading.api.domain.enums.DeliveryStatus
import com.papertrading.api.application.notification.SlackNotificationRequestedEvent
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class SlackNotificationScheduler(
    private val repository: NotificationDeliveryLogRepository,
    private val sender: NotificationSender,
    private val policyStore: SlackNotificationPolicyStore,
    private val retryProcessor: NotificationRetryProcessor,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(fixedDelay = 30_000)
    fun retryPending() {
        val policy = policyStore.get()
        val pendingLogs = repository.findByStatusAndAttemptCountLessThan(
            status = DeliveryStatus.PENDING,
            maxAttempts = policy.maxRetries,
        )

        for (deliveryLog in pendingLogs) {
            try {
                val claimedLog = retryProcessor.markDelivering(deliveryLog)
                if (claimedLog == null) {
                    log.debug { "Delivery log id=${deliveryLog.id} already claimed by another instance, skipping" }
                    continue
                }

                val event = SlackNotificationRequestedEvent(
                    eventType = claimedLog.eventType,
                    accountId = 0L,
                    orderId = null,
                    executionId = null,
                    riskEventId = null,
                    message = claimedLog.message,
                    occurredAt = Instant.now(),
                )

                val success = sender.send(event)
                if (success) {
                    retryProcessor.markDelivered(claimedLog)
                } else {
                    retryProcessor.markFailed(claimedLog, "Slack webhook returned failure", policy.maxRetries)
                }
            } catch (ex: Exception) {
                log.error(ex) { "Error retrying delivery log id=${deliveryLog.id}, continuing to next record" }
                try {
                    retryProcessor.markFailed(deliveryLog, ex.message ?: "Unknown error", policy.maxRetries)
                } catch (saveEx: Exception) {
                    log.error(saveEx) { "Failed to update delivery log after retry error: id=${deliveryLog.id}" }
                }
            }
        }
    }
}
