package com.papertrading.api.infrastructure.notification

import com.papertrading.api.domain.enums.NotificationEventType
import com.papertrading.api.application.notification.SlackNotificationRequestedEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.time.Instant

class SlackWebhookNotifierTest {

    private val restTemplate = mockk<RestTemplate>()

    private fun makeEvent() = SlackNotificationRequestedEvent(
        eventType = NotificationEventType.EXECUTION_FILLED,
        accountId = 1L,
        orderId = 10L,
        executionId = 100L,
        riskEventId = null,
        message = "체결 알림",
        occurredAt = Instant.now(),
    )

    @Test
    fun `webhookUrl이 공백이면 false를 반환하고 restTemplate을 호출하지 않는다`() {
        val props = SlackNotificationProperties(
            enabled = true,
            webhookUrl = "   ",
            timeoutMillis = 1000,
            maxRetries = 1,
            retryBackoffMillis = 1,
            enabledTypes = listOf(NotificationEventType.EXECUTION_FILLED),
        )
        val notifier = SlackWebhookNotifier(restTemplate, props)

        val result = notifier.send(makeEvent())

        assertFalse(result)
        verify(exactly = 0) {
            restTemplate.postForEntity(any<String>(), any(), any<Class<String>>())
        }
    }

    @Test
    fun `전송 성공 시 true를 반환한다`() {
        val props = SlackNotificationProperties(
            enabled = true,
            webhookUrl = "https://hooks.slack.com/services/test",
            timeoutMillis = 1000,
            maxRetries = 1,
            retryBackoffMillis = 1,
            enabledTypes = listOf(NotificationEventType.EXECUTION_FILLED),
        )
        every {
            restTemplate.postForEntity(props.webhookUrl, any<HttpEntity<Map<String, String>>>(), String::class.java)
        } returns ResponseEntity("ok", HttpStatus.OK)

        val notifier = SlackWebhookNotifier(restTemplate, props)

        val result = notifier.send(makeEvent())

        assertTrue(result)
    }

    @Test
    fun `전송 예외 발생 시 false를 반환한다`() {
        val props = SlackNotificationProperties(
            enabled = true,
            webhookUrl = "https://hooks.slack.com/services/test",
            timeoutMillis = 1000,
            maxRetries = 1,
            retryBackoffMillis = 1,
            enabledTypes = listOf(NotificationEventType.EXECUTION_FILLED),
        )
        every {
            restTemplate.postForEntity(props.webhookUrl, any<HttpEntity<Map<String, String>>>(), String::class.java)
        } throws RuntimeException("connection refused")

        val notifier = SlackWebhookNotifier(restTemplate, props)

        val result = notifier.send(makeEvent())

        assertFalse(result)
    }

    @Test
    fun `restTemplate 호출은 정확히 1회만 수행된다 (재시도 루프 없음)`() {
        val props = SlackNotificationProperties(
            enabled = true,
            webhookUrl = "https://hooks.slack.com/services/test",
            timeoutMillis = 1000,
            maxRetries = 5, // maxRetries가 높아도 1회만 시도해야 한다
            retryBackoffMillis = 1,
            enabledTypes = listOf(NotificationEventType.EXECUTION_FILLED),
        )
        every {
            restTemplate.postForEntity(props.webhookUrl, any<HttpEntity<Map<String, String>>>(), String::class.java)
        } throws RuntimeException("boom")

        val notifier = SlackWebhookNotifier(restTemplate, props)
        notifier.send(makeEvent())

        verify(exactly = 1) {
            restTemplate.postForEntity(props.webhookUrl, any<HttpEntity<Map<String, String>>>(), String::class.java)
        }
    }
}
