package com.papertrading.api.infrastructure.notification

import com.papertrading.api.application.notification.SlackNotificationPolicyStore
import com.papertrading.api.application.notification.SlackNotificationRequestedEvent
import com.papertrading.api.domain.entity.notification.Notification
import com.papertrading.api.domain.enums.DeliveryStatus
import com.papertrading.api.domain.enums.NotificationEventType
import com.papertrading.api.infrastructure.persistence.NotificationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class SlackNotificationEventHandlerTest {

    private val sender = mockk<NotificationSender>()
    private val repository = mockk<NotificationRepository>()
    private val notificationSaver = mockk<NotificationSaver>()

    private fun makeEvent(
        eventType: NotificationEventType = NotificationEventType.EXECUTION_FILLED,
    ) = SlackNotificationRequestedEvent(
        eventType = eventType,
        accountId = 1L,
        orderId = 10L,
        executionId = 100L,
        riskEventId = null,
        message = "체결 알림",
        occurredAt = Instant.now(),
    )

    private fun makeProperties(
        enabled: Boolean = true,
        enabledTypes: List<NotificationEventType> = listOf(NotificationEventType.EXECUTION_FILLED),
    ) = SlackNotificationProperties(
        enabled = enabled,
        webhookUrl = "https://hooks.slack.com/services/test",
        timeoutMillis = 1000,
        maxRetries = 3,
        retryBackoffMillis = 1,
        enabledTypes = enabledTypes,
    )

    private fun makeSavedNotification() = Notification.create(
        eventType = NotificationEventType.EXECUTION_FILLED,
        sourceRef = "execution_filled:100",
        title = NotificationEventType.EXECUTION_FILLED.name,
        message = "체결 알림",
    )

    @Test
    fun `policy enabled가 false이면 notificationSaver save와 sender send를 호출하지 않는다`() {
        val props = makeProperties(enabled = false)
        val policyStore = SlackNotificationPolicyStore(props)
        val handler = SlackNotificationEventHandler(sender, policyStore, repository, notificationSaver)

        handler.handle(makeEvent())

        verify(exactly = 0) { notificationSaver.save(any()) }
        verify(exactly = 0) { sender.send(any()) }
    }

    @Test
    fun `eventType이 enabledTypes에 없으면 notificationSaver save를 호출하지 않는다`() {
        val props = makeProperties(
            enabled = true,
            enabledTypes = listOf(NotificationEventType.ORDER_ERROR), // EXECUTION_FILLED는 허용되지 않음
        )
        val policyStore = SlackNotificationPolicyStore(props)
        val handler = SlackNotificationEventHandler(sender, policyStore, repository, notificationSaver)

        handler.handle(makeEvent(eventType = NotificationEventType.EXECUTION_FILLED))

        verify(exactly = 0) { notificationSaver.save(any()) }
        verify(exactly = 0) { sender.send(any()) }
    }

    @Test
    fun `즉시 전송 성공 시 notificationSaver save 1회 + repository save 1회 (DELIVERED 업데이트) 및 sender 1회 호출`() {
        val props = makeProperties()
        val policyStore = SlackNotificationPolicyStore(props)
        val handler = SlackNotificationEventHandler(sender, policyStore, repository, notificationSaver)

        val savedNotification = makeSavedNotification()
        every { notificationSaver.save(any()) } returns savedNotification
        every { repository.save(any()) } answers { firstArg() }
        every { sender.send(any()) } returns true

        handler.handle(makeEvent())

        // notificationSaver.save: PENDING 저장 (REQUIRES_NEW via separate component)
        verify(exactly = 1) { notificationSaver.save(any()) }
        // repository.save: DELIVERED 업데이트
        verify(exactly = 1) { repository.save(any()) }
        verify(exactly = 1) { sender.send(any()) }
    }

    @Test
    fun `즉시 전송 실패 시 notificationSaver save 1회만 호출 (PENDING 저장만), repository save 없음, status는 PENDING 유지`() {
        val props = makeProperties()
        val policyStore = SlackNotificationPolicyStore(props)
        val handler = SlackNotificationEventHandler(sender, policyStore, repository, notificationSaver)

        val savedNotification = makeSavedNotification()
        val savedSlot = slot<Notification>()
        every { notificationSaver.save(capture(savedSlot)) } returns savedNotification
        every { sender.send(any()) } returns false

        handler.handle(makeEvent())

        // 전송 실패 시 PENDING 저장 1회만 — DELIVERED 업데이트는 없음
        verify(exactly = 1) { notificationSaver.save(any()) }
        verify(exactly = 0) { repository.save(any()) }
        verify(exactly = 1) { sender.send(any()) }

        assertEquals(DeliveryStatus.PENDING, savedSlot.captured.status)
    }
}
