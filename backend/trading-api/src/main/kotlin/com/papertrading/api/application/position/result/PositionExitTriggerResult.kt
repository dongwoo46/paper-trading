package com.papertrading.api.application.position.result

import com.papertrading.api.domain.entity.position.PositionExitTrigger
import com.papertrading.api.domain.enums.PriceBasisPolicy
import com.papertrading.api.domain.enums.TriggerSkipReason
import com.papertrading.api.domain.enums.TriggerState
import com.papertrading.api.domain.enums.TriggerType
import java.math.BigDecimal
import java.time.Instant

data class PositionExitTriggerResult(
    val id: Long,
    val positionId: Long,
    val accountId: Long,
    val ticker: String,
    val triggerType: TriggerType,
    val triggerPercent: BigDecimal?,
    val triggerPrice: BigDecimal?,
    val priceBasisPolicy: PriceBasisPolicy,
    val exitRatioPercent: BigDecimal,
    val state: TriggerState,
    val skipReason: TriggerSkipReason?,
    val version: Long,
    val createdAt: Instant?,
    val updatedAt: Instant?,
) {
    companion object {
        fun from(trigger: PositionExitTrigger): PositionExitTriggerResult =
            PositionExitTriggerResult(
                id = requireNotNull(trigger.id) { "trigger.id is null" },
                positionId = trigger.positionId,
                accountId = trigger.accountId,
                ticker = trigger.ticker,
                triggerType = trigger.triggerType,
                triggerPercent = trigger.triggerPercent,
                triggerPrice = trigger.triggerPrice,
                priceBasisPolicy = trigger.priceBasisPolicy,
                exitRatioPercent = trigger.exitRatioPercent,
                state = trigger.state,
                skipReason = trigger.skipReason,
                version = trigger.version,
                createdAt = trigger.createdAt,
                updatedAt = trigger.updatedAt,
            )
    }
}

data class PositionExitTriggerListResult(
    val positionId: Long,
    val triggers: List<PositionExitTriggerResult>,
)
