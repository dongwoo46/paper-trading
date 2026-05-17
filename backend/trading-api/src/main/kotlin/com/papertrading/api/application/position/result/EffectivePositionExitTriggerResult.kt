package com.papertrading.api.application.position.result

import java.math.BigDecimal

data class EffectivePositionExitTriggerResult(
    val positionId: Long,
    val source: String,
    val enabled: Boolean,
    val stopLossPercent: BigDecimal?,
    val takeProfitPercent: BigDecimal?,
    val version: Long,
)
