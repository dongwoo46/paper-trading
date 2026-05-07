package com.papertrading.api.infrastructure.notification

import com.papertrading.api.domain.enums.NotificationEventType
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

class SlackWebhookNotifierTest {

    private val restTemplate = mockk<RestTemplate>()
    private val testMessage = "체결 알림"

    private fun makeProps(webhookUrl: String = "https://hooks.slack.com/services/test") =
        SlackNotificationProperties(
            enabled = true,
            webhookUrl = webhookUrl,
            timeoutMillis = 1000,
            maxRetries = 3,
            retryBackoffMillis = 1,
            enabledTypes = listOf(NotificationEventType.EXECUTION_FILLED),
        )

    @Test
    fun `webhookUrl이 공백이면 false를 반환하고 restTemplate을 호출하지 않는다`() {
        val notifier = SlackWebhookNotifier(restTemplate, makeProps(webhookUrl = "   "))

        assertFalse(notifier.send(testMessage))
        verify(exactly = 0) { restTemplate.postForEntity(any<String>(), any(), any<Class<String>>()) }
    }

    @Test
    fun `전송 성공 시 true를 반환한다`() {
        val props = makeProps()
        every {
            restTemplate.postForEntity(props.webhookUrl, any<HttpEntity<Map<String, String>>>(), String::class.java)
        } returns ResponseEntity("ok", HttpStatus.OK)

        assertTrue(SlackWebhookNotifier(restTemplate, props).send(testMessage))
    }

    @Test
    fun `전송 예외 발생 시 false를 반환한다`() {
        val props = makeProps()
        every {
            restTemplate.postForEntity(props.webhookUrl, any<HttpEntity<Map<String, String>>>(), String::class.java)
        } throws RuntimeException("connection refused")

        assertFalse(SlackWebhookNotifier(restTemplate, props).send(testMessage))
    }

    @Test
    fun `restTemplate 호출은 정확히 1회만 수행된다 (재시도 루프 없음)`() {
        val props = makeProps()
        every {
            restTemplate.postForEntity(props.webhookUrl, any<HttpEntity<Map<String, String>>>(), String::class.java)
        } throws RuntimeException("boom")

        SlackWebhookNotifier(restTemplate, props).send(testMessage)

        verify(exactly = 1) {
            restTemplate.postForEntity(props.webhookUrl, any<HttpEntity<Map<String, String>>>(), String::class.java)
        }
    }
}
