package com.papertrading.api.infrastructure.notification

import com.papertrading.api.application.notification.SlackNotificationPolicyStore
import com.papertrading.api.domain.entity.NotificationDeliveryLog
import com.papertrading.api.domain.enums.DeliveryStatus
import com.papertrading.api.domain.event.SlackNotificationRequestedEvent
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SlackNotificationEventHandler(
    private val sender: NotificationSender,
    private val policyStore: SlackNotificationPolicyStore,
    private val repository: NotificationDeliveryLogRepository,
    private val logSaver: NotificationDeliveryLogSaver,
) {
    private val log = KotlinLogging.logger {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("slackNotificationExecutor")
    fun handle(event: SlackNotificationRequestedEvent) {
        val policy = policyStore.get()
        if (!policy.enabled) return
        if (event.eventType !in policy.enabledTypes) return

        val sourceRef = buildSourceRef(event)
        val deliveryLog = logSaver.save(
            NotificationDeliveryLog(
                eventType = event.eventType,
                sourceRef = sourceRef,
                status = DeliveryStatus.PENDING,
                message = event.message,
            ),
        )

        val success = sender.send(event)
        if (success) {
            val updated = deliveryLog.markDelivered()
            repository.save(updated)
        } else {
            log.warn { "Slack send failed on first attempt — leaving PENDING for scheduler retry: sourceRef=$sourceRef" }
        }
    }

    private fun buildSourceRef(event: SlackNotificationRequestedEvent): String {
        val id = event.executionId ?: event.orderId ?: event.riskEventId ?: 0L
        return "${event.eventType.name.lowercase()}:$id"
    }
}
