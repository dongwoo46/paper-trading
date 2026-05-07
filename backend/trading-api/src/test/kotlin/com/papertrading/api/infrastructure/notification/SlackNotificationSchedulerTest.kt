package com.papertrading.api.infrastructure.notification

import com.papertrading.api.domain.entity.notification.Notification
import com.papertrading.api.domain.enums.NotificationEventType
import com.papertrading.api.domain.port.NotificationSender
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class SlackNotificationSchedulerTest {

    private val sender = mockk<NotificationSender>()
    private val txService = mockk<NotificationPersistenceTxService>()

    private fun makeProperties(maxRetries: Int = 3) = SlackNotificationProperties(
        enabled = true,
        webhookUrl = "https://hooks.slack.com/services/test",
        timeoutMillis = 1000,
        maxRetries = maxRetries,
        retryBackoffMillis = 1,
        enabledTypes = listOf(NotificationEventType.EXECUTION_FILLED),
    )

    private fun makeDisabledProperties() = SlackNotificationProperties(
        enabled = false,
        webhookUrl = "https://hooks.slack.com/services/test",
        timeoutMillis = 1000,
        maxRetries = 3,
        retryBackoffMillis = 1,
        enabledTypes = listOf(NotificationEventType.EXECUTION_FILLED),
    )

    private fun makeNotification(id: Long = 1L): Notification =
        Notification.create(
            eventType = NotificationEventType.EXECUTION_FILLED,
            sourceRef = "execution:$id",
            title = NotificationEventType.EXECUTION_FILLED.name,
            message = "체결 알림 $id",
        )

    @Test
    fun `PENDING 레코드가 없으면 sender send를 호출하지 않는다`() {
        val props = makeProperties()
        val policyStore = SlackNotificationPolicyStore(props)
        val scheduler = SlackNotificationScheduler(sender, policyStore, txService)

        every { txService.findPendingForRetry(props.maxRetries) } returns emptyList()

        scheduler.retryPending()

        verify(exactly = 0) { sender.send(any()) }
    }

    @Test
    fun `policy enabled가 false이면 조회 및 전송을 수행하지 않는다`() {
        val policyStore = SlackNotificationPolicyStore(makeDisabledProperties())
        val scheduler = SlackNotificationScheduler(sender, policyStore, txService)

        scheduler.retryPending()

        verify(exactly = 0) { txService.findPendingForRetry(any()) }
        verify(exactly = 0) { sender.send(any()) }
    }

    @Test
    fun `PENDING 레코드가 있고 전송 성공 시 markDelivering 후 markDelivered가 호출된다`() {
        val props = makeProperties()
        val policyStore = SlackNotificationPolicyStore(props)
        val scheduler = SlackNotificationScheduler(sender, policyStore, txService)

        val notification = makeNotification()
        every { txService.findPendingForRetry(props.maxRetries) } returns listOf(notification)
        every { txService.markDelivering(notification) } answers {
            notification.markDelivering()
            notification
        }
        justRun { txService.markDelivered(any()) }
        every { sender.send(any()) } returns true

        scheduler.retryPending()

        verify(exactly = 1) { txService.markDelivering(notification) }
        verify(exactly = 1) { txService.markDelivered(any()) }
        verify(exactly = 0) { txService.markFailed(any(), any(), any()) }
    }

    @Test
    fun `PENDING 레코드가 있고 전송 실패 시 markDelivering 후 markFailed가 호출된다`() {
        val maxRetries = 3
        val props = makeProperties(maxRetries = maxRetries)
        val policyStore = SlackNotificationPolicyStore(props)
        val scheduler = SlackNotificationScheduler(sender, policyStore, txService)

        val notification = makeNotification()
        every { txService.findPendingForRetry(maxRetries) } returns listOf(notification)
        every { txService.markDelivering(notification) } answers {
            notification.markDelivering()
            notification
        }
        justRun { txService.markFailed(any(), any(), any()) }
        every { sender.send(any()) } returns false

        scheduler.retryPending()

        verify(exactly = 1) { txService.markDelivering(notification) }
        verify(exactly = 1) { txService.markFailed(notification, "Slack webhook returned failure", maxRetries) }
        verify(exactly = 0) { txService.markDelivered(any()) }
    }

    @Test
    fun `claimForDelivery가 0을 반환하면 (다른 인스턴스가 선점) 해당 레코드를 건너뛴다`() {
        val props = makeProperties()
        val policyStore = SlackNotificationPolicyStore(props)
        val scheduler = SlackNotificationScheduler(sender, policyStore, txService)

        val notification = makeNotification()
        every { txService.findPendingForRetry(props.maxRetries) } returns listOf(notification)
        every { txService.markDelivering(notification) } returns null

        scheduler.retryPending()

        verify(exactly = 0) { sender.send(any()) }
        verify(exactly = 0) { txService.markDelivered(any()) }
        verify(exactly = 0) { txService.markFailed(any(), any(), any()) }
    }

    @Test
    fun `하나의 레코드에서 예외 발생 시 나머지 레코드는 계속 처리된다 (격리)`() {
        val props = makeProperties()
        val policyStore = SlackNotificationPolicyStore(props)
        val scheduler = SlackNotificationScheduler(sender, policyStore, txService)

        val failingNotification = makeNotification(id = 1L)
        val successNotification = makeNotification(id = 2L)

        every { txService.findPendingForRetry(props.maxRetries) } returns listOf(failingNotification, successNotification)

        every { txService.markDelivering(failingNotification) } throws RuntimeException("slack 서버 다운")
        justRun { txService.markFailed(failingNotification, any(), any()) }

        every { txService.markDelivering(successNotification) } answers {
            successNotification.markDelivering()
            successNotification
        }
        every { sender.send("체결 알림 2") } returns true
        justRun { txService.markDelivered(successNotification) }

        scheduler.retryPending()

        verify(exactly = 1) { txService.markDelivered(successNotification) }
        verify(exactly = 1) { txService.markFailed(failingNotification, any(), any()) }
        verify(exactly = 1) { sender.send(any()) }
    }

    @Test
    fun `markFailed에 올바른 maxRetries가 전달된다`() {
        val maxRetries = 3
        val props = makeProperties(maxRetries = maxRetries)
        val policyStore = SlackNotificationPolicyStore(props)
        val scheduler = SlackNotificationScheduler(sender, policyStore, txService)

        val notification = makeNotification()
        every { txService.findPendingForRetry(maxRetries) } returns listOf(notification)
        every { txService.markDelivering(notification) } answers {
            notification.markDelivering()
            notification
        }
        justRun { txService.markFailed(any(), any(), any()) }
        every { sender.send(any()) } returns false

        scheduler.retryPending()

        verify(exactly = 1) { txService.markFailed(notification, "Slack webhook returned failure", maxRetries) }
    }
}
