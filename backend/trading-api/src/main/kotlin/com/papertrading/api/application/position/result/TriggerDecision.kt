package com.papertrading.api.application.position.result

import com.papertrading.api.domain.enums.TriggerType
import java.math.BigDecimal
import java.time.Instant

data class TriggerDecision(
    val triggerId: Long,
    val triggerVersion: Long,
    val positionId: Long,
    val triggerType: TriggerType,
    val effectiveTriggerPrice: BigDecimal,
    val quotePrice: BigDecimal,
    val quoteAt: Instant,
    val exitRatioPercent: BigDecimal,
)

