package com.papertrading.api.application.account.command

import java.math.BigDecimal

data class UpsertAccountExitTriggerDefaultCommand(
    val accountId: Long,
    val enabled: Boolean,
    val stopLossPercent: BigDecimal?,
    val takeProfitPercent: BigDecimal?,
)
