package com.papertrading.api.interfaces.rest.position.dto

import com.papertrading.api.application.position.EffectivePositionExitTriggerResult
import com.papertrading.api.application.position.PositionExitTriggerResult
import com.papertrading.api.application.position.UpsertPositionExitTriggerCommand
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
    val expectedTriggerVersion: Long? = null,
) {
    fun toCommand(positionId: Long) = UpsertPositionExitTriggerCommand(positionId, enabled, stopLossPercent, takeProfitPercent, expectedTriggerVersion)
}

data class PositionExitTriggerResponse(
    val positionId: Long,
    val enabled: Boolean,
    val stopLossPercent: BigDecimal?,
    val takeProfitPercent: BigDecimal?,
    val triggerVersion: Long,
    val updatedAt: Instant?,
)

data class EffectivePositionExitTriggerResponse(
    val positionId: Long,
    val source: String,
    val enabled: Boolean,
    val stopLossPercent: BigDecimal?,
    val takeProfitPercent: BigDecimal?,
    val triggerVersion: Long,
)

fun PositionExitTriggerResult.toResponse() = PositionExitTriggerResponse(positionId, enabled, stopLossPercent, takeProfitPercent, triggerVersion, updatedAt)
fun EffectivePositionExitTriggerResult.toResponse() = EffectivePositionExitTriggerResponse(positionId, source, enabled, stopLossPercent, takeProfitPercent, triggerVersion)
