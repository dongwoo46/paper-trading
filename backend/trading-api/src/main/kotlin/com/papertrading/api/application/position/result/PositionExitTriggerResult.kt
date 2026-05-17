package com.papertrading.api.application.position.result

import java.math.BigDecimal
import java.time.Instant

data class PositionExitTriggerResult(
    val positionId: Long,
    val enabled: Boolean,
    val stopLossPercent: BigDecimal?,
    val takeProfitPercent: BigDecimal?,
    val version: Long,
    val updatedAt: Instant?,
)
