package com.papertrading.api.application.position

import com.papertrading.api.application.position.command.CancelPositionExitTriggerCommand
import com.papertrading.api.application.position.command.CreatePositionExitTriggerCommand
import com.papertrading.api.application.position.command.UpdatePositionExitTriggerCommand
import com.papertrading.api.application.position.result.PositionExitTriggerResult
import com.papertrading.api.common.exception.BadRequestException
import com.papertrading.api.common.exception.NotFoundException
import com.papertrading.api.common.exception.PositionNotEligibleException
import com.papertrading.api.common.exception.PositionNotFoundException
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.entity.position.PositionExitTrigger
import com.papertrading.api.domain.enums.PriceBasisPolicy
import com.papertrading.api.domain.enums.TriggerType
import com.papertrading.api.infrastructure.persistence.PositionExitTriggerRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

// 포지션 자동 청산 조건을 단일 트리거 단위로 생성, 수정, 취소한다.
@Service
class PositionExitTriggerCommandService(
    private val positionRepository: PositionRepository,
    private val positionExitTriggerRepository: PositionExitTriggerRepository,
) {
    @Transactional
    fun createPositionTrigger(command: CreatePositionExitTriggerCommand): PositionExitTriggerResult {
        val position = findOpenPositionForUpdate(command.positionId)
        val resolved = resolveTriggerPolicy(
            position = position,
            triggerType = command.triggerType,
            triggerPercent = command.triggerPercent,
            triggerPrice = command.triggerPrice,
            priceBasisPolicy = command.priceBasisPolicy,
        )
        val trigger = PositionExitTrigger.create(
            positionId = position.id!!,
            accountId = position.account.id!!,
            ticker = position.ticker,
            triggerType = command.triggerType,
            triggerPercent = resolved.triggerPercent,
            triggerPrice = resolved.triggerPrice,
            priceBasisPolicy = command.priceBasisPolicy,
            exitRatioPercent = command.exitRatioPercent,
        )
        return PositionExitTriggerResult.from(positionExitTriggerRepository.save(trigger))
    }

    @Transactional
    fun updatePositionTrigger(command: UpdatePositionExitTriggerCommand): PositionExitTriggerResult {
        val position = findOpenPositionForUpdate(command.positionId)
        val trigger = findTriggerForUpdate(command.triggerId, command.positionId)
        val resolved = resolveTriggerPolicy(
            position = position,
            triggerType = trigger.triggerType,
            triggerPercent = command.triggerPercent,
            triggerPrice = command.triggerPrice,
            priceBasisPolicy = command.priceBasisPolicy,
        )
        trigger.update(
            triggerPercent = resolved.triggerPercent,
            triggerPrice = resolved.triggerPrice,
            priceBasisPolicy = command.priceBasisPolicy,
            exitRatioPercent = command.exitRatioPercent ?: trigger.exitRatioPercent,
            expectedVersion = command.expectedVersion,
        )
        return PositionExitTriggerResult.from(positionExitTriggerRepository.save(trigger))
    }

    @Transactional
    fun cancelPositionTrigger(command: CancelPositionExitTriggerCommand): PositionExitTriggerResult {
        findOpenPositionForUpdate(command.positionId)
        val trigger = findTriggerForUpdate(command.triggerId, command.positionId)
        trigger.cancel(command.expectedVersion)
        return PositionExitTriggerResult.from(positionExitTriggerRepository.save(trigger))
    }

    private fun findOpenPositionForUpdate(positionId: Long): Position {
        val position = positionRepository.findByIdWithLock(positionId)
            .orElseThrow { PositionNotFoundException(positionId = positionId) }
        if (position.quantity <= BigDecimal.ZERO) {
            throw PositionNotEligibleException("position not eligible: already closed")
        }
        return position
    }

    private fun findTriggerForUpdate(triggerId: Long, positionId: Long): PositionExitTrigger {
        val trigger = positionExitTriggerRepository.findByIdForUpdate(triggerId)
            ?: throw exitTriggerNotFound(positionId, triggerId)
        if (trigger.positionId != positionId) {
            throw exitTriggerNotFound(positionId, triggerId)
        }
        return trigger
    }

    private fun resolveTriggerPolicy(
        position: Position,
        triggerType: TriggerType,
        triggerPercent: BigDecimal?,
        triggerPrice: BigDecimal?,
        priceBasisPolicy: PriceBasisPolicy,
    ): ResolvedTriggerPolicy =
        when (priceBasisPolicy) {
            PriceBasisPolicy.FIXED_PRICE -> {
                if (triggerPrice == null) {
                    throw BadRequestException("TRIGGER_PRICE_REQUIRED", "FIXED_PRICE requires triggerPrice")
                }
                ResolvedTriggerPolicy(triggerPercent, triggerPrice)
            }
            PriceBasisPolicy.AVG_PRICE_AT_CREATION -> {
                if (triggerPrice != null) {
                    throw BadRequestException(
                        "INVALID_PRICE_BASIS_POLICY",
                        "triggerPrice can be supplied only with FIXED_PRICE",
                    )
                }
                val percent = triggerPercent
                    ?: throw BadRequestException("TRIGGER_PERCENT_REQUIRED", "AVG_PRICE_AT_CREATION requires triggerPercent")
                ResolvedTriggerPolicy(percent, thresholdPrice(position.avgBuyPrice, percent, triggerType))
            }
            PriceBasisPolicy.FOLLOW_AVG_PRICE -> {
                if (triggerPrice != null) {
                    throw BadRequestException(
                        "INVALID_PRICE_BASIS_POLICY",
                        "triggerPrice can be supplied only with FIXED_PRICE",
                    )
                }
                val percent = triggerPercent
                    ?: throw BadRequestException("TRIGGER_PERCENT_REQUIRED", "FOLLOW_AVG_PRICE requires triggerPercent")
                ResolvedTriggerPolicy(percent, null)
            }
        }

    private fun thresholdPrice(avgBuyPrice: BigDecimal, percent: BigDecimal, triggerType: TriggerType): BigDecimal {
        if (avgBuyPrice <= BigDecimal.ZERO) {
            throw BadRequestException("INVALID_AVG_BUY_PRICE", "avgBuyPrice must be positive to derive triggerPrice")
        }
        val ratio = percent.divide(HUNDRED)
        val multiplier = when (triggerType) {
            TriggerType.STOP_LOSS -> BigDecimal.ONE.subtract(ratio)
            TriggerType.TAKE_PROFIT -> BigDecimal.ONE.add(ratio)
        }
        return avgBuyPrice.multiply(multiplier).setScale(4, RoundingMode.HALF_UP)
    }

    private fun exitTriggerNotFound(positionId: Long, triggerId: Long): NotFoundException =
        NotFoundException(
            "POSITION_EXIT_TRIGGER_NOT_FOUND",
            "position exit trigger not found. positionId=$positionId, triggerId=$triggerId",
        )

    private data class ResolvedTriggerPolicy(
        val triggerPercent: BigDecimal?,
        val triggerPrice: BigDecimal?,
    )

    companion object {
        private val HUNDRED = BigDecimal("100")
    }
}
