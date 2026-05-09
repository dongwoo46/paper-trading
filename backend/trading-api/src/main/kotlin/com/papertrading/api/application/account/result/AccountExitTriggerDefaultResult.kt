package com.papertrading.api.application.account.result

import java.math.BigDecimal
import java.time.Instant

data class AccountExitTriggerDefaultResult(
    val accountId: Long,
    val enabled: Boolean,
    val stopLossPercent: BigDecimal?,
    val takeProfitPercent: BigDecimal?,
    val updatedAt: Instant?,
)
