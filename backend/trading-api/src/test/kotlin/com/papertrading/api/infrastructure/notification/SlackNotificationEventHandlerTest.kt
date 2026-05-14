package com.papertrading.api.infrastructure.notification

import com.papertrading.api.application.notification.SlackNotificationRequestedEvent
import com.papertrading.api.domain.entity.notification.Notification
import com.papertrading.api.domain.enums.DeliveryStatus
import com.papertrading.api.domain.enums.NotificationEventType
import com.papertrading.api.domain.port.NotificationSender
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verifyOrder
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class SlackNotificationEventHandlerTest {

    private val sender = mockk<NotificationSender>()
    private val txService = mockk<NotificationPersistenceTxService>()

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
    fun `policy enabled가 false이면 txService save와 sender send를 호출하지 않는다`() {
        val props = makeProperties(enabled = false)
        val policyStore = SlackNotificationConfigStore(props)
        val handler = SlackNotificationEventHandler(sender, policyStore, txService)

        handler.handle(makeEvent())

        verify(exactly = 0) { txService.save(any()) }
        verify(exactly = 0) { sender.send(any()) }
    }

    @Test
    fun `eventType이 enabledTypes에 없으면 txService save를 호출하지 않는다`() {
        val props = makeProperties(
            enabled = true,
            enabledTypes = listOf(NotificationEventType.ORDER_ERROR),
        )
        val policyStore = SlackNotificationConfigStore(props)
        val handler = SlackNotificationEventHandler(sender, policyStore, txService)

        handler.handle(makeEvent(eventType = NotificationEventType.EXECUTION_FILLED))

        verify(exactly = 0) { txService.save(any()) }
        verify(exactly = 0) { sender.send(any()) }
    }

    @Test
    fun `즉시 전송 성공 시 txService save 1회 + markDelivered 1회 및 sender 1회 호출`() {
        val props = makeProperties()
        val policyStore = SlackNotificationConfigStore(props)
        val handler = SlackNotificationEventHandler(sender, policyStore, txService)

        val savedNotification = makeSavedNotification()
        every { txService.save(any()) } returns savedNotification
        justRun { txService.markDelivered(any()) }
        every { sender.send(any()) } returns true

        handler.handle(makeEvent())

        verify(exactly = 1) { txService.save(any()) }
        verify(exactly = 1) { txService.markDelivered(any()) }
        verify(exactly = 1) { sender.send(any()) }
    }

    @Test
    fun `즉시 전송 실패 시 txService save 1회만 호출 (PENDING 저장만), markDelivered 없음, status는 PENDING 유지`() {
        val props = makeProperties()
        val policyStore = SlackNotificationConfigStore(props)
        val handler = SlackNotificationEventHandler(sender, policyStore, txService)

        val savedNotification = makeSavedNotification()
        val savedSlot = slot<Notification>()
        every { txService.save(capture(savedSlot)) } returns savedNotification
        every { sender.send(any()) } returns false

        handler.handle(makeEvent())

        verify(exactly = 1) { txService.save(any()) }
        verify(exactly = 0) { txService.markDelivered(any()) }
        verify(exactly = 1) { sender.send(any()) }

        assertEquals(DeliveryStatus.PENDING, savedSlot.captured.status)
    }

    @Test
    fun `즉시 전송 중 예외 발생 시 markDelivered를 호출하지 않고 예외를 전파한다`() {
        val props = makeProperties()
        val policyStore = SlackNotificationConfigStore(props)
        val handler = SlackNotificationEventHandler(sender, policyStore, txService)

        val savedNotification = makeSavedNotification()
        every { txService.save(any()) } returns savedNotification
        every { sender.send(any()) } throws RuntimeException("slack unavailable")

        org.junit.jupiter.api.assertThrows<RuntimeException> {
            handler.handle(makeEvent())
        }

        verifyOrder {
            txService.save(any())
            sender.send(any())
        }
        verify(exactly = 0) { txService.markDelivered(any()) }
    }
}
