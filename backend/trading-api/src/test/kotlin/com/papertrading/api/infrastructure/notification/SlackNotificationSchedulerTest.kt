package com.papertrading.api.infrastructure.notification

import com.papertrading.api.application.notification.SlackNotificationPolicyStore
import com.papertrading.api.domain.entity.notification.NotificationDeliveryLog
import com.papertrading.api.domain.enums.DeliveryStatus
import com.papertrading.api.domain.enums.NotificationEventType
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SlackNotificationSchedulerTest {

    private val repository = mockk<NotificationDeliveryLogRepository>()
    private val sender = mockk<NotificationSender>()
    private val retryProcessor = mockk<NotificationRetryProcessor>()

    private fun makeProperties(maxRetries: Int = 3) = SlackNotificationProperties(
        enabled = true,
        webhookUrl = "https://hooks.slack.com/services/test",
        timeoutMillis = 1000,
        maxRetries = maxRetries,
        retryBackoffMillis = 1,
        enabledTypes = listOf(NotificationEventType.EXECUTION_FILLED),
    )

    private fun makeLog(
        id: Long = 1L,
        attemptCount: Int = 0,
        status: DeliveryStatus = DeliveryStatus.PENDING,
    ) = NotificationDeliveryLog(
        id = id,
        eventType = NotificationEventType.EXECUTION_FILLED,
        sourceRef = "execution:$id",
        status = status,
        attemptCount = attemptCount,
        message = "체결 알림 $id",
    )

    @Test
    fun `PENDING 레코드가 없으면 sender send를 호출하지 않는다`() {
        val props = makeProperties()
        val policyStore = SlackNotificationPolicyStore(props)
        val scheduler = SlackNotificationScheduler(repository, sender, policyStore, retryProcessor)

        every {
            repository.findByStatusAndAttemptCountLessThan(DeliveryStatus.PENDING, props.maxRetries)
        } returns emptyList()

        scheduler.retryPending()

        verify(exactly = 0) { sender.send(any()) }
    }

    @Test
    fun `PENDING 레코드가 있고 전송 성공 시 markDelivering 후 markDelivered가 호출된다`() {
        val props = makeProperties()
        val policyStore = SlackNotificationPolicyStore(props)
        val scheduler = SlackNotificationScheduler(repository, sender, policyStore, retryProcessor)

        val log = makeLog()
        every {
            repository.findByStatusAndAttemptCountLessThan(DeliveryStatus.PENDING, props.maxRetries)
        } returns listOf(log)
        every { retryProcessor.markDelivering(log) } answers {
            log.markDelivering()
            log
        }
        justRun { retryProcessor.markDelivered(any()) }
        every { sender.send(any()) } returns true

        scheduler.retryPending()

        verify(exactly = 1) { retryProcessor.markDelivering(log) }
        verify(exactly = 1) { retryProcessor.markDelivered(any()) }
        verify(exactly = 0) { retryProcessor.markFailed(any(), any(), any()) }
    }

    @Test
    fun `PENDING 레코드가 있고 전송 실패 시 markDelivering 후 markFailed가 호출된다`() {
        val maxRetries = 3
        val props = makeProperties(maxRetries = maxRetries)
        val policyStore = SlackNotificationPolicyStore(props)
        val scheduler = SlackNotificationScheduler(repository, sender, policyStore, retryProcessor)

        val log = makeLog(attemptCount = 0)
        every {
            repository.findByStatusAndAttemptCountLessThan(DeliveryStatus.PENDING, maxRetries)
        } returns listOf(log)
        every { retryProcessor.markDelivering(log) } answers {
            log.markDelivering()
            log
        }
        justRun { retryProcessor.markFailed(any(), any(), any()) }
        every { sender.send(any()) } returns false

        scheduler.retryPending()

        verify(exactly = 1) { retryProcessor.markDelivering(log) }
        verify(exactly = 1) { retryProcessor.markFailed(log, "Slack webhook returned failure", maxRetries) }
        verify(exactly = 0) { retryProcessor.markDelivered(any()) }
    }

    @Test
    fun `claimForDelivery가 0을 반환하면 (다른 인스턴스가 선점) 해당 레코드를 건너뛴다`() {
        val props = makeProperties()
        val policyStore = SlackNotificationPolicyStore(props)
        val scheduler = SlackNotificationScheduler(repository, sender, policyStore, retryProcessor)

        val log = makeLog()
        every {
            repository.findByStatusAndAttemptCountLessThan(DeliveryStatus.PENDING, props.maxRetries)
        } returns listOf(log)
        // markDelivering returns null — already claimed by another instance
        every { retryProcessor.markDelivering(log) } returns null

        scheduler.retryPending()

        verify(exactly = 0) { sender.send(any()) }
        verify(exactly = 0) { retryProcessor.markDelivered(any()) }
        verify(exactly = 0) { retryProcessor.markFailed(any(), any(), any()) }
    }

    @Test
    fun `하나의 레코드에서 예외 발생 시 나머지 레코드는 계속 처리된다 (격리)`() {
        val props = makeProperties()
        val policyStore = SlackNotificationPolicyStore(props)
        val scheduler = SlackNotificationScheduler(repository, sender, policyStore, retryProcessor)

        val failingLog = makeLog(id = 1L)
        val successLog = makeLog(id = 2L)

        every {
            repository.findByStatusAndAttemptCountLessThan(DeliveryStatus.PENDING, props.maxRetries)
        } returns listOf(failingLog, successLog)

        // 첫 번째 레코드의 markDelivering은 예외를 던짐
        every { retryProcessor.markDelivering(failingLog) } throws RuntimeException("slack 서버 다운")
        justRun { retryProcessor.markFailed(failingLog, any(), any()) }

        // 두 번째 레코드는 성공
        every { retryProcessor.markDelivering(successLog) } answers {
            successLog.markDelivering()
            successLog
        }
        every { sender.send(match { it.message == "체결 알림 2" }) } returns true
        justRun { retryProcessor.markDelivered(successLog) }

        scheduler.retryPending()

        // 두 번째 레코드는 성공적으로 처리되어야 한다
        verify(exactly = 1) { retryProcessor.markDelivered(successLog) }
        // 첫 번째 레코드는 markFailed가 호출되어야 한다
        verify(exactly = 1) { retryProcessor.markFailed(failingLog, any(), any()) }
        verify(exactly = 1) { sender.send(any()) }
    }

    @Test
    fun `attemptCount가 maxRetries - 1일 때 markFailed에 올바른 maxRetries가 전달된다`() {
        val maxRetries = 3
        val props = makeProperties(maxRetries = maxRetries)
        val policyStore = SlackNotificationPolicyStore(props)
        val scheduler = SlackNotificationScheduler(repository, sender, policyStore, retryProcessor)

        val log = makeLog(attemptCount = maxRetries - 1)
        every {
            repository.findByStatusAndAttemptCountLessThan(DeliveryStatus.PENDING, maxRetries)
        } returns listOf(log)
        every { retryProcessor.markDelivering(log) } answers {
            log.markDelivering()
            log
        }
        justRun { retryProcessor.markFailed(any(), any(), any()) }
        every { sender.send(any()) } returns false

        scheduler.retryPending()

        verify(exactly = 1) { retryProcessor.markFailed(log, "Slack webhook returned failure", maxRetries) }
    }
}
