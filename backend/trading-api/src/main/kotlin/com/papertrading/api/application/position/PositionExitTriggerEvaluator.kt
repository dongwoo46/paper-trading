package com.papertrading.api.application.position

import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.position.PositionExitTrigger
import com.papertrading.api.domain.position.TriggerState
import com.papertrading.api.domain.position.TriggerType
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

data class TriggerDecision(val type: TriggerType, val thresholdPrice: BigDecimal, val quotedPrice: BigDecimal, val decidedAt: Instant)

@Component
class PositionExitTriggerEvaluator {
    fun evaluate(position: Position, trigger: PositionExitTrigger, quotePrice: BigDecimal, now: Instant): TriggerDecision? {
        if (!trigger.enabled) return null
        if (trigger.triggeredBy != null) return null
        val entry = position.avgBuyPrice
        trigger.stopLossPercent?.let {
            if (trigger.stopLossState == TriggerState.ARMED) {
                val threshold = entry.multiply(BigDecimal.ONE.subtract(it.divide(BigDecimal("100"))))
                if (quotePrice <= threshold) return TriggerDecision(TriggerType.STOP_LOSS, threshold, quotePrice, now)
            }
        }
        trigger.takeProfitPercent?.let {
            if (trigger.takeProfitState == TriggerState.ARMED) {
                val threshold = entry.multiply(BigDecimal.ONE.add(it.divide(BigDecimal("100"))))
                if (quotePrice >= threshold) return TriggerDecision(TriggerType.TAKE_PROFIT, threshold, quotePrice, now)
            }
        }
        return null
    }
}

