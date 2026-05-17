package com.papertrading.api.application.position.result

import com.papertrading.api.domain.enums.TriggerType
import java.math.BigDecimal
import java.time.Instant

data class TriggerDecision(val type: TriggerType, val thresholdPrice: BigDecimal, val quotedPrice: BigDecimal, val decidedAt: Instant)

