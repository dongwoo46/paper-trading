package com.papertrading.api.application.position

import com.papertrading.api.domain.position.PositionExitTrigger
import com.papertrading.api.domain.position.PositionExitTriggerRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

data class UpsertPositionExitTriggerCommand(
    val positionId: Long,
    val enabled: Boolean,
    val stopLossPercent: BigDecimal?,
    val takeProfitPercent: BigDecimal?,
    val expectedTriggerVersion: Long? = null,
)
data class PositionExitTriggerResult(
    val positionId: Long,
    val enabled: Boolean,
    val stopLossPercent: BigDecimal?,
    val takeProfitPercent: BigDecimal?,
    val triggerVersion: Long,
    val updatedAt: Instant?,
)

@Service
class PositionExitTriggerCommandService(
    private val positionRepository: PositionRepository,
    private val positionExitTriggerRepository: PositionExitTriggerRepository,
) {
    @Transactional
    fun upsertPositionTrigger(command: UpsertPositionExitTriggerCommand): PositionExitTriggerResult {
        val position = positionRepository.findById(command.positionId).orElseThrow { NoSuchElementException("position not found") }
        if (position.quantity <= BigDecimal.ZERO) {
            throw PositionNotEligibleException("position not eligible: already closed")
        }
        validatePercentScale(command.stopLossPercent)
        validatePercentScale(command.takeProfitPercent)
        val current = positionExitTriggerRepository.findByPositionId(command.positionId)
        val entity = if (current == null) {
            PositionExitTrigger.create(position.id!!, position.account.id!!, position.ticker, command.enabled, command.stopLossPercent, command.takeProfitPercent)
        } else {
            if (command.expectedTriggerVersion != null && command.expectedTriggerVersion != current.triggerVersion) {
                throw StaleTriggerVersionException(
                    "stale trigger version: expected=${command.expectedTriggerVersion}, actual=${current.triggerVersion}"
                )
            }
            current.enabled = command.enabled
            current.stopLossPercent = command.stopLossPercent
            current.takeProfitPercent = command.takeProfitPercent
            current.triggerVersion += 1
            current.validate()
            current
        }
        val saved = positionExitTriggerRepository.save(entity)
        return PositionExitTriggerResult(saved.positionId, saved.enabled, saved.stopLossPercent, saved.takeProfitPercent, saved.triggerVersion, saved.updatedAt)
    }

    private fun validatePercentScale(value: BigDecimal?) {
        if (value != null && value.stripTrailingZeros().scale() > 4) {
            throw IllegalArgumentException("percent scale must be <= 4")
        }
    }
}
