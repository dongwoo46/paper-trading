package com.papertrading.api.application.position

import com.papertrading.api.application.position.command.UpsertPositionExitTriggerCommand
import com.papertrading.api.application.position.result.PositionExitTriggerResult
import com.papertrading.api.common.exception.PositionNotEligibleException
import com.papertrading.api.common.exception.PositionNotFoundException
import com.papertrading.api.domain.entity.position.PositionExitTrigger
import com.papertrading.api.infrastructure.persistence.PositionExitTriggerRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

// 포지션 자동 청산 조건 생성 및 수정
@Service
class PositionExitTriggerCommandService(
    private val positionRepository: PositionRepository,
    private val positionExitTriggerRepository: PositionExitTriggerRepository,
) {
    @Transactional
    fun upsertPositionTrigger(command: UpsertPositionExitTriggerCommand): PositionExitTriggerResult {
        val current = positionExitTriggerRepository.findByPositionIdForUpdate(command.positionId)
        val position = positionRepository.findByIdWithLock(command.positionId)
            .orElseThrow { PositionNotFoundException(positionId = command.positionId) }
        if (position.quantity <= BigDecimal.ZERO) {
            throw PositionNotEligibleException("position not eligible: already closed")
        }

        val entity = if (current == null) {
            PositionExitTrigger.create(position.id!!, position.account.id!!, position.ticker, command.enabled, command.stopLossPercent, command.takeProfitPercent)
        } else {
            current.upsertPolicy(
                enabled = command.enabled,
                stopLossPercent = command.stopLossPercent,
                takeProfitPercent = command.takeProfitPercent,
                expectedVersion = command.expectedVersion,
            )
            current
        }
        val saved = positionExitTriggerRepository.save(entity)
        return PositionExitTriggerResult(saved.positionId, saved.enabled, saved.stopLossPercent, saved.takeProfitPercent, saved.version, saved.updatedAt)
    }
}
