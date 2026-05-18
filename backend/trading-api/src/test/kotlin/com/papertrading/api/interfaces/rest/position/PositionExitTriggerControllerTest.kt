package com.papertrading.api.interfaces.rest.position

import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.api.application.position.PositionExitTriggerCommandService
import com.papertrading.api.application.position.PositionExitTriggerQueryService
import com.papertrading.api.application.position.command.CancelPositionExitTriggerCommand
import com.papertrading.api.application.position.command.CreatePositionExitTriggerCommand
import com.papertrading.api.application.position.command.UpdatePositionExitTriggerCommand
import com.papertrading.api.application.position.result.PositionExitTriggerListResult
import com.papertrading.api.application.position.result.PositionExitTriggerResult
import com.papertrading.api.common.exception.PositionNotEligibleException
import com.papertrading.api.domain.enums.PriceBasisPolicy
import com.papertrading.api.domain.enums.TriggerState
import com.papertrading.api.domain.enums.TriggerType
import com.papertrading.api.presentation.controller.PositionExitTriggerController
import com.papertrading.api.presentation.exception.GlobalExceptionHandler
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
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
    fun `create API accepts single trigger DTO and returns new trigger shape`() {
        val command = CreatePositionExitTriggerCommand(
            positionId = 10L,
            triggerType = TriggerType.STOP_LOSS,
            triggerPercent = null,
            triggerPrice = BigDecimal("95.0000"),
            priceBasisPolicy = PriceBasisPolicy.FIXED_PRICE,
            exitRatioPercent = BigDecimal("50.0000"),
        )
        given(commandService.createPositionTrigger(command)).willReturn(
            triggerResult(
                id = 100L,
                triggerType = TriggerType.STOP_LOSS,
                triggerPercent = null,
                triggerPrice = BigDecimal("95.0000"),
                priceBasisPolicy = PriceBasisPolicy.FIXED_PRICE,
                exitRatioPercent = BigDecimal("50.0000"),
            )
        )

        mockMvc.post("/api/positions/10/exit-triggers") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "triggerType" to "STOP_LOSS",
                    "triggerPrice" to "95.0000",
                    "priceBasisPolicy" to "FIXED_PRICE",
                    "exitRatioPercent" to "50.0000",
                )
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(100) }
            jsonPath("$.triggerType") { value("STOP_LOSS") }
            jsonPath("$.triggerPrice") { value(95.0000) }
            jsonPath("$.priceBasisPolicy") { value("FIXED_PRICE") }
            jsonPath("$.exitRatioPercent") { value(50.0000) }
            jsonPath("$.state") { value("ARMED") }
        }
    }

    @Test
    fun `list API returns plural trigger collection`() {
        given(queryService.listPositionTriggers(10L)).willReturn(
            PositionExitTriggerListResult(
                positionId = 10L,
                triggers = listOf(
                    triggerResult(
                        id = 100L,
                        triggerType = TriggerType.STOP_LOSS,
                        triggerPercent = null,
                        triggerPrice = BigDecimal("95.0000"),
                        priceBasisPolicy = PriceBasisPolicy.FIXED_PRICE,
                        exitRatioPercent = BigDecimal("100.0000"),
                    ),
                    triggerResult(
                        id = 101L,
                        triggerType = TriggerType.TAKE_PROFIT,
                        triggerPercent = BigDecimal("7.0000"),
                        triggerPrice = BigDecimal("107.0000"),
                        priceBasisPolicy = PriceBasisPolicy.AVG_PRICE_AT_CREATION,
                        exitRatioPercent = BigDecimal("25.0000"),
                    ),
                ),
            )
        )

        mockMvc.get("/api/positions/10/exit-triggers")
            .andExpect {
                status { isOk() }
                jsonPath("$.positionId") { value(10) }
                jsonPath("$.triggers", hasSize<Any>(2)) {}
                jsonPath("$.triggers[0].triggerType") { value("STOP_LOSS") }
                jsonPath("$.triggers[1].priceBasisPolicy") { value("AVG_PRICE_AT_CREATION") }
            }
    }

    @Test
    fun `update API sends expected version and ratio to command service`() {
        val command = UpdatePositionExitTriggerCommand(
            positionId = 10L,
            triggerId = 100L,
            triggerPercent = BigDecimal("8.0000"),
            triggerPrice = null,
            priceBasisPolicy = PriceBasisPolicy.FOLLOW_AVG_PRICE,
            exitRatioPercent = BigDecimal("75.0000"),
            expectedVersion = 3L,
        )
        given(commandService.updatePositionTrigger(command)).willReturn(
            triggerResult(
                id = 100L,
                triggerType = TriggerType.STOP_LOSS,
                triggerPercent = BigDecimal("8.0000"),
                triggerPrice = null,
                priceBasisPolicy = PriceBasisPolicy.FOLLOW_AVG_PRICE,
                exitRatioPercent = BigDecimal("75.0000"),
                version = 4L,
            )
        )

        mockMvc.patch("/api/positions/10/exit-triggers/100") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "triggerPercent" to "8.0000",
                    "priceBasisPolicy" to "FOLLOW_AVG_PRICE",
                    "exitRatioPercent" to "75.0000",
                    "expectedVersion" to 3,
                )
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$.triggerPercent") { value(8.0000) }
            jsonPath("$.triggerPrice") { doesNotExist() }
            jsonPath("$.priceBasisPolicy") { value("FOLLOW_AVG_PRICE") }
            jsonPath("$.version") { value(4) }
        }
    }

    @Test
    fun `delete API passes expected version query parameter`() {
        val command = CancelPositionExitTriggerCommand(positionId = 10L, triggerId = 100L, expectedVersion = 3L)
        given(commandService.cancelPositionTrigger(command)).willReturn(
            triggerResult(
                id = 100L,
                triggerType = TriggerType.STOP_LOSS,
                triggerPercent = null,
                triggerPrice = BigDecimal("95.0000"),
                priceBasisPolicy = PriceBasisPolicy.FIXED_PRICE,
                exitRatioPercent = BigDecimal("100.0000"),
                state = TriggerState.CANCELED,
            )
        )

        mockMvc.delete("/api/positions/10/exit-triggers/100?expectedVersion=3")
            .andExpect {
                status { isOk() }
                jsonPath("$.state") { value("CANCELED") }
            }
    }

    @Test
    fun `closed position create request returns 422`() {
        given(
            commandService.createPositionTrigger(
                CreatePositionExitTriggerCommand(
                    positionId = 10L,
                    triggerType = TriggerType.STOP_LOSS,
                    triggerPercent = null,
                    triggerPrice = BigDecimal("95.0000"),
                    priceBasisPolicy = PriceBasisPolicy.FIXED_PRICE,
                    exitRatioPercent = null,
                )
            )
        ).willThrow(PositionNotEligibleException("position not eligible"))

        mockMvc.post("/api/positions/10/exit-triggers") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "triggerType" to "STOP_LOSS",
                    "triggerPrice" to "95.0000",
                    "priceBasisPolicy" to "FIXED_PRICE",
                )
            )
        }.andExpect { status { isUnprocessableEntity() } }
    }

    private fun triggerResult(
        id: Long,
        triggerType: TriggerType,
        triggerPercent: BigDecimal?,
        triggerPrice: BigDecimal?,
        priceBasisPolicy: PriceBasisPolicy,
        exitRatioPercent: BigDecimal,
        state: TriggerState = TriggerState.ARMED,
        version: Long = 0L,
    ): PositionExitTriggerResult =
        PositionExitTriggerResult(
            id = id,
            positionId = 10L,
            accountId = 1L,
            ticker = "005930",
            triggerType = triggerType,
            triggerPercent = triggerPercent,
            triggerPrice = triggerPrice,
            priceBasisPolicy = priceBasisPolicy,
            exitRatioPercent = exitRatioPercent,
            state = state,
            skipReason = null,
            version = version,
            createdAt = Instant.parse("2026-05-08T12:00:00Z"),
            updatedAt = Instant.parse("2026-05-08T12:00:00Z"),
        )
}
