package com.papertrading.api.application.account.command

import java.math.BigDecimal

data class UpsertRiskPolicyCommand(
    val maxPositionRatio: BigDecimal?,
    val maxDailyLoss: BigDecimal?,
    val maxOrderAmount: BigDecimal?
)
