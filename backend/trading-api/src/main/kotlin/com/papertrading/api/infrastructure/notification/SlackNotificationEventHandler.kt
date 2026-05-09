package com.papertrading.api.infrastructure.notification

import com.papertrading.api.application.notification.port.SlackNotificationConfig
import com.papertrading.api.application.notification.SlackNotificationRequestedEvent
import com.papertrading.api.domain.entity.notification.Notification
import com.papertrading.api.domain.port.NotificationSender
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SlackNotificationEventHandler(
    private val sender: NotificationSender,
    private val policyStore: SlackNotificationConfig,
    private val txService: NotificationPersistenceTxService,
) {
    private val log = KotlinLogging.logger {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("slackNotificationExecutor")
    fun handle(event: SlackNotificationRequestedEvent) {
        val policy = policyStore.get()
        if (!policy.enabled) return
        if (event.eventType !in policy.enabledTypes) return

        val sourceRef = buildSourceRef(event)
        val notification = txService.save(
            Notification.create(
                eventType = event.eventType,
                sourceRef = sourceRef,
                title = event.eventType.name,
                message = event.message,
            ),
        )

        val success = sender.send(event.message)
        if (success) {
            txService.markDelivered(notification)
        } else {
            log.warn { "Slack send failed on first attempt — leaving PENDING for scheduler retry: sourceRef=$sourceRef" }
        }
    }

    private fun buildSourceRef(event: SlackNotificationRequestedEvent): String {
        val id = event.executionId ?: event.orderId ?: event.riskEventId ?: 0L
        return "${event.eventType.name.lowercase()}:$id"
    }
}
