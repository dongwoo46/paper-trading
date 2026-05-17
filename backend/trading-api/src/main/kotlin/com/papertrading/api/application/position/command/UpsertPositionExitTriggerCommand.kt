package com.papertrading.api.application.position.command

import java.math.BigDecimal

data class UpsertPositionExitTriggerCommand(
    val positionId: Long,
    val enabled: Boolean,
    val stopLossPercent: BigDecimal?,
    val takeProfitPercent: BigDecimal?,
    val expectedVersion: Long? = null,
)
