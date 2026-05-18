package com.papertrading.api.application.position.command

import com.papertrading.api.domain.enums.PriceBasisPolicy
import com.papertrading.api.domain.enums.TriggerType
import java.math.BigDecimal

data class CreatePositionExitTriggerCommand(
    val positionId: Long,
    val triggerType: TriggerType,
    val triggerPercent: BigDecimal?,
    val triggerPrice: BigDecimal?,
    val priceBasisPolicy: PriceBasisPolicy,
    val exitRatioPercent: BigDecimal? = null,
)

data class UpdatePositionExitTriggerCommand(
    val positionId: Long,
    val triggerId: Long,
    val triggerPercent: BigDecimal?,
    val triggerPrice: BigDecimal?,
    val priceBasisPolicy: PriceBasisPolicy,
    val exitRatioPercent: BigDecimal? = null,
    val expectedVersion: Long? = null,
)

data class CancelPositionExitTriggerCommand(
    val positionId: Long,
    val triggerId: Long,
    val expectedVersion: Long? = null,
)
