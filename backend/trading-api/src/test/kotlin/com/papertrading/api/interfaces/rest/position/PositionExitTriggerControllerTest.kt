package com.papertrading.api.interfaces.rest.position

import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.api.application.position.command.UpsertPositionExitTriggerCommand
import com.papertrading.api.application.position.result.EffectivePositionExitTriggerResult
import com.papertrading.api.application.position.result.PositionExitTriggerResult
import com.papertrading.api.application.position.PositionExitTriggerCommandService
import com.papertrading.api.application.position.PositionExitTriggerQueryService
import com.papertrading.api.common.exception.PositionNotEligibleException
import com.papertrading.api.presentation.controller.PositionExitTriggerController
import com.papertrading.api.presentation.exception.GlobalExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import java.math.BigDecimal
import java.time.Instant

@WebMvcTest(PositionExitTriggerController::class)
@Import(GlobalExceptionHandler::class)
class PositionExitTriggerControllerTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @MockBean lateinit var commandService: PositionExitTriggerCommandService
    @MockBean lateinit var queryService: PositionExitTriggerQueryService

    @Test
    fun `업서트와 조회 API 동작`() {
        given(commandService.upsertPositionTrigger(UpsertPositionExitTriggerCommand(10L, true, BigDecimal("2.0"), BigDecimal("6.0")))).willReturn(
            PositionExitTriggerResult(10L, true, BigDecimal("2.0"), BigDecimal("6.0"), 3, Instant.now())
        )
        given(queryService.getEffectiveTrigger(10L)).willReturn(EffectivePositionExitTriggerResult(10L, "POSITION_OVERRIDE", true, BigDecimal("2.0"), BigDecimal("6.0"), 3))

        mockMvc.put("/api/positions/10/exit-trigger") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("enabled" to true, "stopLossPercent" to "2.0", "takeProfitPercent" to "6.0"))
        }.andExpect { status { isOk() } }

        mockMvc.get("/api/positions/10/exit-trigger")
            .andExpect { status { isOk() } }
    }

    @Test
    fun `closed position 요청은 422를 반환해야 한다`() {
        given(commandService.upsertPositionTrigger(UpsertPositionExitTriggerCommand(10L, true, BigDecimal("2.0"), BigDecimal("6.0"))))
            .willThrow(PositionNotEligibleException("position not eligible"))

        mockMvc.put("/api/positions/10/exit-trigger") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("enabled" to true, "stopLossPercent" to "2.0", "takeProfitPercent" to "6.0"))
        }.andExpect { status { isUnprocessableEntity() } }
    }
}
