package com.papertrading.api.application.position

import com.papertrading.api.application.position.command.UpsertPositionExitTriggerCommand
import com.papertrading.api.application.position.result.PositionExitTriggerResult
import com.papertrading.api.common.exception.PositionNotEligibleException
import com.papertrading.api.common.exception.PositionNotFoundException
import com.papertrading.api.domain.entity.position.PositionExitTrigger
import com.papertrading.api.domain.enums.PriceBasisPolicy
import com.papertrading.api.domain.enums.TriggerType
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
        val position = positionRepository.findByIdWithLock(command.positionId)
            .orElseThrow { PositionNotFoundException(positionId = command.positionId) }
        if (position.quantity <= BigDecimal.ZERO) {
            throw PositionNotEligibleException("position not eligible: already closed")
        }

        val existing = positionExitTriggerRepository.findAllByPositionIdOrderByIdAsc(command.positionId)
        existing.filter { it.state == com.papertrading.api.domain.enums.TriggerState.ARMED }
            .forEach { it.cancel(command.expectedVersion) }

        val created = if (command.enabled) {
            listOfNotNull(
                command.stopLossPercent?.let { percent ->
                    PositionExitTrigger.create(
                        position.id!!,
                        position.account.id!!,
                        position.ticker,
                        TriggerType.STOP_LOSS,
                        percent,
                        null,
                        PriceBasisPolicy.FOLLOW_AVG_PRICE,
                    )
                },
                command.takeProfitPercent?.let { percent ->
                    PositionExitTrigger.create(
                        position.id!!,
                        position.account.id!!,
                        position.ticker,
                        TriggerType.TAKE_PROFIT,
                        percent,
                        null,
                        PriceBasisPolicy.FOLLOW_AVG_PRICE,
                    )
                },
            )
        } else {
            emptyList()
        }
        val saved = positionExitTriggerRepository.saveAll(existing + created)
        val active = saved.filter { it.state == com.papertrading.api.domain.enums.TriggerState.ARMED }
        return PositionExitTriggerResult(
            position.id!!,
            active.isNotEmpty(),
            active.firstOrNull { it.triggerType == TriggerType.STOP_LOSS }?.triggerPercent,
            active.firstOrNull { it.triggerType == TriggerType.TAKE_PROFIT }?.triggerPercent,
            active.maxOfOrNull { it.version } ?: 0,
            active.maxByOrNull { it.updatedAt ?: java.time.Instant.EPOCH }?.updatedAt,
        )
    }
}
