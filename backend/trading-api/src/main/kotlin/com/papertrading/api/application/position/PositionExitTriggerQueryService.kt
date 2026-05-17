package com.papertrading.api.application.position

import com.papertrading.api.application.position.result.EffectivePositionExitTriggerResult
import com.papertrading.api.common.exception.PositionNotFoundException
import com.papertrading.api.infrastructure.persistence.PositionExitTriggerRepository
import com.papertrading.api.infrastructure.persistence.AccountExitTriggerDefaultRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal


// 특정 포지션에 실제로 적용되는 청산 트리거 설정을 조회하는 Query Service
// 수정필요함 redis에서 가져오고
@Service
class PositionExitTriggerQueryService(
    private val positionRepository: PositionRepository,
    private val positionExitTriggerRepository: PositionExitTriggerRepository,
    private val accountExitTriggerDefaultRepository: AccountExitTriggerDefaultRepository,
) {
    fun getEffectiveTrigger(positionId: Long): EffectivePositionExitTriggerResult {
        val position = positionRepository.findById(positionId)
            .orElseThrow { PositionNotFoundException(positionId = positionId) }
        val override = positionExitTriggerRepository.findByPositionId(positionId)
        if (override != null) return EffectivePositionExitTriggerResult(
            positionId,
            "POSITION_OVERRIDE",
            override.enabled,
            override.stopLossPercent,
            override.takeProfitPercent,
            override.version
        )
        val accountDefault = accountExitTriggerDefaultRepository.findByAccountId(position.account.id!!)
        if (accountDefault != null) return EffectivePositionExitTriggerResult(positionId, "ACCOUNT_DEFAULT", accountDefault.enabled, accountDefault.stopLossPercent, accountDefault.takeProfitPercent, 0)
        return EffectivePositionExitTriggerResult(positionId, "DISABLED", false, null, null, 0)
    }
}
