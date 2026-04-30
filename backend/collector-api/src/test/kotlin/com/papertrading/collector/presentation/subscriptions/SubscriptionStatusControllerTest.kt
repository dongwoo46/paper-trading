package com.papertrading.collector.presentation.subscriptions

import com.papertrading.collector.application.subscriptions.dto.SubscriptionModeStatus
import com.papertrading.collector.application.subscriptions.dto.SubscriptionStatusReport
import com.papertrading.collector.application.subscriptions.service.SubscriptionStatusService
import com.papertrading.collector.domain.kis.WsConnectionStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant

class SubscriptionStatusControllerTest {

    private lateinit var statusService: SubscriptionStatusService
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        statusService = mockk()
        val controller = SubscriptionStatusController(statusService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `GET_subscriptions_status_returns_200_with_correct_structure`() {
        val now = Instant.parse("2026-04-30T10:00:00Z")
        val lastConnected = Instant.parse("2026-04-30T09:55:00Z")
        every { statusService.report() } returns SubscriptionStatusReport(
            generatedAt = now,
            modes = listOf(
                SubscriptionModeStatus(
                    mode = "paper",
                    connectionStatus = WsConnectionStatus.CONNECTED,
                    lastConnectedAt = lastConnected,
                    reconnectAttempts = 0,
                    wsSymbols = listOf("005930"),
                    restSymbols = emptyList(),
                    wsSlotUsed = 1,
                    wsSlotMax = 41,
                )
            ),
            totalWsSlotUsed = 1,
            totalWsSlotMax = 41,
        )

        mockMvc.perform(get("/api/subscriptions/status").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.generatedAt").value("2026-04-30T10:00:00Z"))
            .andExpect(jsonPath("$.totalWsSlotUsed").value(1))
            .andExpect(jsonPath("$.totalWsSlotMax").value(41))
            .andExpect(jsonPath("$.modes[0].mode").value("paper"))
            .andExpect(jsonPath("$.modes[0].connectionStatus").value("CONNECTED"))
            .andExpect(jsonPath("$.modes[0].lastConnectedAt").value("2026-04-30T09:55:00Z"))
            .andExpect(jsonPath("$.modes[0].reconnectAttempts").value(0))
            .andExpect(jsonPath("$.modes[0].wsSymbols[0]").value("005930"))
            .andExpect(jsonPath("$.modes[0].restSymbols").isEmpty)
            .andExpect(jsonPath("$.modes[0].wsSlotUsed").value(1))
            .andExpect(jsonPath("$.modes[0].wsSlotMax").value(41))
    }

    @Test
    fun `GET_subscriptions_status_maps_null_lastConnectedAt_to_null_in_json`() {
        every { statusService.report() } returns SubscriptionStatusReport(
            generatedAt = Instant.now(),
            modes = listOf(
                SubscriptionModeStatus(
                    mode = "paper",
                    connectionStatus = WsConnectionStatus.DISCONNECTED,
                    lastConnectedAt = null,
                    reconnectAttempts = 0,
                    wsSymbols = emptyList(),
                    restSymbols = emptyList(),
                    wsSlotUsed = 0,
                    wsSlotMax = 41,
                )
            ),
            totalWsSlotUsed = 0,
            totalWsSlotMax = 41,
        )

        mockMvc.perform(get("/api/subscriptions/status").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.modes[0].lastConnectedAt").value(null as String?))
    }

    @Test
    fun `GET_subscriptions_status_returns_200_with_empty_modes_when_disabled`() {
        every { statusService.report() } returns SubscriptionStatusReport(
            generatedAt = Instant.now(),
            modes = emptyList(),
            totalWsSlotUsed = 0,
            totalWsSlotMax = 41,
        )

        mockMvc.perform(get("/api/subscriptions/status").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.modes").isArray)
            .andExpect(jsonPath("$.modes").isEmpty)
            .andExpect(jsonPath("$.totalWsSlotUsed").value(0))
    }
}
