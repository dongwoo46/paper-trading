package com.papertrading.api.application.position

import com.papertrading.api.domain.account.AccountExitTriggerDefaultRepository
import com.papertrading.api.domain.position.PositionExitTriggerRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal

data class EffectivePositionExitTriggerResult(
    val positionId: Long,
    val source: String,
    val enabled: Boolean,
    val stopLossPercent: BigDecimal?,
    val takeProfitPercent: BigDecimal?,
    val triggerVersion: Long,
)

@Service
class PositionExitTriggerQueryService(
    private val positionRepository: PositionRepository,
    private val positionExitTriggerRepository: PositionExitTriggerRepository,
    private val accountExitTriggerDefaultRepository: AccountExitTriggerDefaultRepository,
) {
    fun getEffectiveTrigger(positionId: Long): EffectivePositionExitTriggerResult {
        val position = positionRepository.findById(positionId).orElseThrow { NoSuchElementException("position not found") }
        val override = positionExitTriggerRepository.findByPositionId(positionId)
        if (override != null) return EffectivePositionExitTriggerResult(positionId, "POSITION_OVERRIDE", override.enabled, override.stopLossPercent, override.takeProfitPercent, override.triggerVersion)
        val accountDefault = accountExitTriggerDefaultRepository.findByAccountId(position.account.id!!)
        if (accountDefault != null) return EffectivePositionExitTriggerResult(positionId, "ACCOUNT_DEFAULT", accountDefault.enabled, accountDefault.stopLossPercent, accountDefault.takeProfitPercent, 1)
        return EffectivePositionExitTriggerResult(positionId, "DISABLED", false, null, null, 0)
    }
}

