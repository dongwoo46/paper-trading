package com.papertrading.api.presentation.dto.position

import com.papertrading.api.application.position.command.CreatePositionExitTriggerCommand
import com.papertrading.api.application.position.command.UpdatePositionExitTriggerCommand
import com.papertrading.api.application.position.result.PositionExitTriggerListResult
import com.papertrading.api.application.position.result.PositionExitTriggerResult
import com.papertrading.api.domain.enums.PriceBasisPolicy
import com.papertrading.api.domain.enums.TriggerSkipReason
import com.papertrading.api.domain.enums.TriggerState
import com.papertrading.api.domain.enums.TriggerType
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import java.math.BigDecimal
import java.time.Instant

data class CreatePositionExitTriggerRequest(
    val triggerType: TriggerType,
    @field:Digits(integer = 3, fraction = 4)
    @field:DecimalMin(value = "0.0001")
    @field:DecimalMax(value = "99.9999")
    val triggerPercent: BigDecimal?,
    @field:Digits(integer = 16, fraction = 4)
    @field:DecimalMin(value = "0.0001")
    val triggerPrice: BigDecimal?,
    val priceBasisPolicy: PriceBasisPolicy,
    @field:Digits(integer = 3, fraction = 4)
    @field:DecimalMin(value = "0.0001")
    @field:DecimalMax(value = "100.0000")
    val exitRatioPercent: BigDecimal? = null,
) {
    fun toCommand(positionId: Long) = CreatePositionExitTriggerCommand(
        positionId = positionId,
        triggerType = triggerType,
        triggerPercent = triggerPercent,
        triggerPrice = triggerPrice,
        priceBasisPolicy = priceBasisPolicy,
        exitRatioPercent = exitRatioPercent,
    )
}

data class UpdatePositionExitTriggerRequest(
    @field:Digits(integer = 3, fraction = 4)
    @field:DecimalMin(value = "0.0001")
    @field:DecimalMax(value = "99.9999")
    val triggerPercent: BigDecimal?,
    @field:Digits(integer = 16, fraction = 4)
    @field:DecimalMin(value = "0.0001")
    val triggerPrice: BigDecimal?,
    val priceBasisPolicy: PriceBasisPolicy,
    @field:Digits(integer = 3, fraction = 4)
    @field:DecimalMin(value = "0.0001")
    @field:DecimalMax(value = "100.0000")
    val exitRatioPercent: BigDecimal? = null,
    val expectedVersion: Long? = null,
) {
    fun toCommand(positionId: Long, triggerId: Long) = UpdatePositionExitTriggerCommand(
        positionId = positionId,
        triggerId = triggerId,
        triggerPercent = triggerPercent,
        triggerPrice = triggerPrice,
        priceBasisPolicy = priceBasisPolicy,
        exitRatioPercent = exitRatioPercent,
        expectedVersion = expectedVersion,
    )
}

data class PositionExitTriggerResponse(
    val id: Long,
    val positionId: Long,
    val accountId: Long,
    val ticker: String,
    val triggerType: TriggerType,
    val triggerPercent: BigDecimal?,
    val triggerPrice: BigDecimal?,
    val priceBasisPolicy: PriceBasisPolicy,
    val exitRatioPercent: BigDecimal,
    val state: TriggerState,
    val skipReason: TriggerSkipReason?,
    val version: Long,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

data class PositionExitTriggerListResponse(
    val positionId: Long,
    val triggers: List<PositionExitTriggerResponse>,
)

fun PositionExitTriggerResult.toResponse() = PositionExitTriggerResponse(
    id = id,
    positionId = positionId,
    accountId = accountId,
    ticker = ticker,
    triggerType = triggerType,
    triggerPercent = triggerPercent,
    triggerPrice = triggerPrice,
    priceBasisPolicy = priceBasisPolicy,
    exitRatioPercent = exitRatioPercent,
    state = state,
    skipReason = skipReason,
    version = version,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PositionExitTriggerListResult.toResponse() = PositionExitTriggerListResponse(
    positionId = positionId,
    triggers = triggers.map { it.toResponse() },
)
