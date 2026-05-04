package com.papertrading.api.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.api.application.notification.SlackNotificationCommandService
import com.papertrading.api.application.notification.SlackNotificationConfigResult
import com.papertrading.api.application.notification.SlackNotificationQueryService
import com.papertrading.api.domain.enums.NotificationEventType
import com.papertrading.api.presentation.exception.GlobalExceptionHandler
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@WebMvcTest(SlackNotificationController::class)
@Import(GlobalExceptionHandler::class, SlackNotificationControllerTest.TestConfig::class)
class SlackNotificationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var queryService: SlackNotificationQueryService

    @Autowired
    private lateinit var commandService: SlackNotificationCommandService

    @Test
    fun `GET config returns current slack notification settings`() {
        every { queryService.getConfig() } returns sampleConfig(enabled = true)

        mockMvc.perform(get("/api/operations/notifications/slack"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.webhookConfigured").value(true))
            .andExpect(jsonPath("$.enabledTypes[0]").value("EXECUTION_FILLED"))
    }

    @Test
    fun `PUT config updates policy and returns updated payload`() {
        every {
            commandService.updateConfig(any())
        } returns sampleConfig(enabled = true)

        val body = mapOf(
            "enabled" to true,
            "maxRetries" to 2,
            "retryBackoffMillis" to 800,
            "enabledTypes" to listOf("EXECUTION_FILLED", "ORDER_ERROR"),
        )

        mockMvc.perform(
            put("/api/operations/notifications/slack")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.maxRetries").value(2))
            .andExpect(jsonPath("$.enabledTypes[1]").value("ORDER_ERROR"))
    }

    @Test
    fun `POST test returns accepted response with requestId`() {
        every { commandService.sendTestMessage("ping") } returns "req-123"

        val body = mapOf("message" to "ping")

        mockMvc.perform(
            post("/api/operations/notifications/slack/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.accepted").value(true))
            .andExpect(jsonPath("$.requestId").value("req-123"))
    }

    @Test
    fun `POST test returns 400 when webhook is not configured`() {
        every { commandService.sendTestMessage(any()) } throws IllegalStateException("WEBHOOK_NOT_CONFIGURED")

        val body = mapOf("message" to "ping")

        mockMvc.perform(
            post("/api/operations/notifications/slack/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }

    private fun sampleConfig(enabled: Boolean): SlackNotificationConfigResult =
        SlackNotificationConfigResult(
            enabled = enabled,
            webhookConfigured = true,
            timeoutMillis = 3000,
            maxRetries = 2,
            retryBackoffMillis = 500,
            enabledTypes = listOf(NotificationEventType.EXECUTION_FILLED, NotificationEventType.ORDER_ERROR),
            updatedAt = Instant.parse("2026-05-04T00:00:00Z"),
        )

    @TestConfiguration
    class TestConfig {
        @Bean
        fun slackNotificationQueryService(): SlackNotificationQueryService = mockk(relaxed = true)

        @Bean
        fun slackNotificationCommandService(): SlackNotificationCommandService = mockk(relaxed = true)
    }
}
