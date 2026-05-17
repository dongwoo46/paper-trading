package com.papertrading.api.application.position

import com.papertrading.api.application.position.result.TriggerDecision
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.entity.position.PositionExitTrigger
import com.papertrading.api.domain.enums.TriggerState
import com.papertrading.api.domain.enums.TriggerType
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

/**
 * 리팩터링 목표
 *
 * 1. PositionExitTrigger를 단일 청산 조건 엔티티로 변경
 * 2. TriggerType(STOP_LOSS / TAKE_PROFIT) 기반으로 트리거 종류 구분
 * 3. triggerPercent, triggerPrice, exitRatioPercent 필드 추가
 * 4. state 하나로 트리거 상태 관리
 * 5. Evaluator는 triggerType에 따라 가격 비교 방향만 다르게 평가
 * 6. 트리거 등록 시 평균매수가 기준 triggerPrice 계산 후 저장
 * 7. 하나의 tick에서 여러 트리거가 동시에 조건을 만족할 수 있으므로, 발동 후보를 positionId + triggerType 기준으로 그룹핑
 * 8. 같은 positionId와 triggerType에서 동시에 발동한 트리거들은 개별 주문으로 분리하지 않고 exitRatioPercent를 합산하여 하나의 매도 주문으로 처리
 * 9. triggerPrice는 발동 조건 가격으로만 사용하고, 실제 주문 기준가는 tick의 quotePrice를 사용
 * 10. 각 트리거는 triggerId + entity version 기반 멱등성 키로 중복 실행을 방지하고, 같은 tick에서 묶인 주문은 orderGroupId로 추적
 */
@Component
class PositionExitTriggerEvaluator {
    fun evaluate(position: Position, trigger: PositionExitTrigger, quotePrice: BigDecimal, now: Instant): TriggerDecision? {
        if (!trigger.enabled) return null
        if (trigger.triggeredBy != null) return null

        trigger.stopLossPercent?.let { percent ->
            if (trigger.stopLossState == TriggerState.ARMED) {
                val threshold = stopLossThreshold(position.avgBuyPrice, percent)
                if (quotePrice <= threshold) return TriggerDecision(TriggerType.STOP_LOSS, threshold, quotePrice, now)
            }
        }

        trigger.takeProfitPercent?.let { percent ->
            if (trigger.takeProfitState == TriggerState.ARMED) {
                val threshold = takeProfitThreshold(position.avgBuyPrice, percent)
                if (quotePrice >= threshold) return TriggerDecision(TriggerType.TAKE_PROFIT, threshold, quotePrice, now)
            }
        }

        return null
    }

    private fun stopLossThreshold(entryPrice: BigDecimal, percent: BigDecimal): BigDecimal =
        entryPrice.multiply(BigDecimal.ONE.subtract(percentRatio(percent)))

    private fun takeProfitThreshold(entryPrice: BigDecimal, percent: BigDecimal): BigDecimal =
        entryPrice.multiply(BigDecimal.ONE.add(percentRatio(percent)))

    private fun percentRatio(percent: BigDecimal): BigDecimal =
        percent.divide(HUNDRED)

    companion object {
        private val HUNDRED = BigDecimal("100")
    }
}

