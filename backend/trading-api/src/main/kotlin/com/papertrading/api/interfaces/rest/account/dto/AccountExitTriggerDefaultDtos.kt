package com.papertrading.api.interfaces.rest.account.dto

import com.papertrading.api.application.account.AccountExitTriggerDefaultResult
import com.papertrading.api.application.account.UpsertAccountExitTriggerDefaultCommand
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import java.math.BigDecimal
import java.time.Instant

data class UpsertAccountExitTriggerDefaultRequest(
    val enabled: Boolean,
    @field:Digits(integer = 3, fraction = 4)
    @field:DecimalMin(value = "0.0001")
    @field:DecimalMax(value = "99.9999")
    val stopLossPercent: BigDecimal?,
    @field:Digits(integer = 3, fraction = 4)
    @field:DecimalMin(value = "0.0001")
    @field:DecimalMax(value = "99.9999")
    val takeProfitPercent: BigDecimal?,
) {
    fun toCommand(accountId: Long) = UpsertAccountExitTriggerDefaultCommand(
        accountId = accountId,
        enabled = enabled,
        stopLossPercent = stopLossPercent,
        takeProfitPercent = takeProfitPercent,
    )
}

data class AccountExitTriggerDefaultResponse(
    val accountId: Long,
    val enabled: Boolean,
    val stopLossPercent: BigDecimal?,
    val takeProfitPercent: BigDecimal?,
    val updatedAt: Instant?,
)

fun AccountExitTriggerDefaultResult.toResponse() = AccountExitTriggerDefaultResponse(
    accountId = accountId,
    enabled = enabled,
    stopLossPercent = stopLossPercent,
    takeProfitPercent = takeProfitPercent,
    updatedAt = updatedAt,
)
