package com.papertrading.api.presentation.dto.position

import com.papertrading.api.application.position.command.UpsertPositionExitTriggerCommand
import com.papertrading.api.application.position.result.EffectivePositionExitTriggerResult
import com.papertrading.api.application.position.result.PositionExitTriggerResult
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import java.math.BigDecimal
import java.time.Instant

data class UpsertPositionExitTriggerRequest(
    val enabled: Boolean,
    @field:Digits(integer = 3, fraction = 4)
    @field:DecimalMin(value = "0.0001")
    @field:DecimalMax(value = "99.9999")
    val stopLossPercent: BigDecimal?,
    @field:Digits(integer = 3, fraction = 4)
    @field:DecimalMin(value = "0.0001")
    @field:DecimalMax(value = "99.9999")
    val takeProfitPercent: BigDecimal?,
    val expectedVersion: Long? = null,
) {
    fun toCommand(positionId: Long) = UpsertPositionExitTriggerCommand(positionId, enabled, stopLossPercent, takeProfitPercent, expectedVersion)
}

data class PositionExitTriggerResponse(
    val positionId: Long,
    val enabled: Boolean,
    val stopLossPercent: BigDecimal?,
    val takeProfitPercent: BigDecimal?,
    val version: Long,
    val updatedAt: Instant?,
)

data class EffectivePositionExitTriggerResponse(
    val positionId: Long,
    val source: String,
    val enabled: Boolean,
    val stopLossPercent: BigDecimal?,
    val takeProfitPercent: BigDecimal?,
    val version: Long,
)

fun PositionExitTriggerResult.toResponse() = PositionExitTriggerResponse(positionId, enabled, stopLossPercent, takeProfitPercent, version, updatedAt)
fun EffectivePositionExitTriggerResult.toResponse() = EffectivePositionExitTriggerResponse(positionId, source, enabled, stopLossPercent, takeProfitPercent, version)
