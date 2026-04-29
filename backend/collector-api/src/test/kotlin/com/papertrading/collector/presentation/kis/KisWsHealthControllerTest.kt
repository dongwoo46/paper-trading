package com.papertrading.collector.presentation.kis

import com.papertrading.collector.application.kis.dto.WsHealthSnapshot
import com.papertrading.collector.application.kis.service.KisWsHealthService
import com.papertrading.collector.domain.kis.WsConnectionStatus
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Instant

@WebMvcTest(KisWsHealthController::class)
class KisWsHealthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var healthService: KisWsHealthService

    @Test
    fun `health_returns_200_with_correct_json_shape`() {
        val lastConnectedAt = Instant.parse("2026-04-29T10:00:00Z")
        given(healthService.health()).willReturn(
            listOf(
                WsHealthSnapshot(
                    mode = "paper",
                    status = WsConnectionStatus.CONNECTED,
                    lastConnectedAt = lastConnectedAt,
                    reconnectAttempts = 0,
                ),
                WsHealthSnapshot(
                    mode = "live",
                    status = WsConnectionStatus.RECONNECTING,
                    lastConnectedAt = Instant.parse("2026-04-29T09:55:00Z"),
                    reconnectAttempts = 2,
                ),
            ),
        )

        mockMvc.get("/api/kis/ws/health")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
                // First element: paper
                jsonPath("$[0].mode") { value("paper") }
                jsonPath("$[0].status") { value("CONNECTED") }
                jsonPath("$[0].lastConnectedAt") { value("2026-04-29T10:00:00Z") }
                jsonPath("$[0].reconnectAttempts") { value(0) }
                // Second element: live
                jsonPath("$[1].mode") { value("live") }
                jsonPath("$[1].status") { value("RECONNECTING") }
                jsonPath("$[1].reconnectAttempts") { value(2) }
                // status must be a string, not an ordinal
                jsonPath("$[0].status") { isString() }
                jsonPath("$[1].status") { isString() }
                // lastConnectedAt must be an ISO-8601 string
                jsonPath("$[1].lastConnectedAt") { isString() }
            }
    }

    @Test
    fun `health_returns_empty_array_when_no_modes`() {
        given(healthService.health()).willReturn(emptyList())

        mockMvc.get("/api/kis/ws/health")
            .andExpect {
                status { isOk() }
                jsonPath("$") { isArray() }
                jsonPath("$.length()") { value(0) }
            }
    }

    @Test
    fun `health_lastConnectedAt_is_null_when_never_connected`() {
        given(healthService.health()).willReturn(
            listOf(
                WsHealthSnapshot(
                    mode = "paper",
                    status = WsConnectionStatus.DISCONNECTED,
                    lastConnectedAt = null,
                    reconnectAttempts = 0,
                ),
            ),
        )

        mockMvc.get("/api/kis/ws/health")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].mode") { value("paper") }
                jsonPath("$[0].status") { value("DISCONNECTED") }
                // lastConnectedAt must be present as null, not absent
                jsonPath("$[0].lastConnectedAt") { value(null as String?) }
                jsonPath("$[0].reconnectAttempts") { value(0) }
            }
    }
}
