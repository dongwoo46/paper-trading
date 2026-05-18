package com.papertrading.api.application.position

import com.papertrading.api.application.notification.SlackNotificationEventPublisher
import com.papertrading.api.application.order.AutoExitTriggerAuditInput
import com.papertrading.api.application.position.result.TriggerDecision
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.enums.TriggerSkipReason
import com.papertrading.api.domain.enums.TriggerState
import com.papertrading.api.domain.enums.TriggerType
import com.papertrading.api.infrastructure.persistence.OrderRepository
import com.papertrading.api.infrastructure.persistence.PositionExitTriggerRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

// 시세가 들어왔을 때 포지션의 청산 트리거를 평가하고, 필요하면 자동 청산 주문 실행을 지시
@Service
class PositionExitTriggerOrchestrator(
    private val positionRepository: PositionRepository,
    private val triggerRepository: PositionExitTriggerRepository,
    private val evaluator: PositionExitTriggerEvaluator,
    private val autoExitOrderPlacementService: AutoExitOrderPlacementService,
    private val orderRepository: OrderRepository,
    private val notificationEventPublisher: SlackNotificationEventPublisher,
) {
    private val positionLocks = ConcurrentHashMap<Long, Any>()
    private val maxOrderCreateRetries = 3

    @Transactional
    fun onQuote(ticker: String, price: BigDecimal, quoteAt: Instant) {
        val normalizedTicker = ticker.uppercase()
        val groups = triggerRepository.findByTickerAndState(normalizedTicker, TriggerState.ARMED)
            .groupBy { TriggerGroupKey(it.positionId, it.triggerType) }

        groups.forEach { (groupKey, candidates) ->
            try {
                processGroup(normalizedTicker, groupKey, price, quoteAt)
            } catch (ex: CannotAcquireLockException) {
                markCandidatesSkipped(candidates, quoteAt, price, TriggerSkipReason.LOCK_CONFLICT)
            } catch (ex: PessimisticLockingFailureException) {
                markCandidatesSkipped(candidates, quoteAt, price, TriggerSkipReason.LOCK_CONFLICT)
            }
        }
    }

    private fun processGroup(ticker: String, groupKey: TriggerGroupKey, price: BigDecimal, quoteAt: Instant) {
        synchronized(positionLocks.computeIfAbsent(groupKey.positionId) { Any() }) {
            val position = positionRepository.findByIdWithLock(groupKey.positionId).orElse(null)
            val triggers = triggerRepository.findArmedGroupForUpdate(ticker, groupKey.positionId, groupKey.triggerType)
            if (triggers.isEmpty()) return

            if (position == null || position.quantity <= BigDecimal.ZERO) {
                markTriggersSkipped(triggers, quoteAt, price, TriggerSkipReason.POSITION_CLOSED)
                return
            }

            val decisions = triggers.mapNotNull { trigger ->
                val decision = evaluator.evaluate(position, trigger, price, quoteAt)
                if (decision == null) trigger.recordEvaluation(quoteAt, price)
                decision
            }
            if (decisions.isEmpty()) {
                triggerRepository.saveAll(triggers)
                return
            }

            val firedTriggers = triggers.filter { trigger ->
                decisions.any { it.triggerId == trigger.id }
            }

            val accountId = position.account.id ?: run {
                markTriggersFailed(firedTriggers, quoteAt, price)
                return
            }

            if (orderRepository.existsOpenOrderByAccountTickerSideWithoutGroup(accountId, position.ticker)) {
                markTriggersSkipped(firedTriggers, quoteAt, price, TriggerSkipReason.MANUAL_SELL_CONFLICT)
                return
            }

            val pendingSellQuantity = orderRepository.sumOpenSellQuantity(accountId, position.ticker)
            val availableQuantity = position.quantity.subtract(pendingSellQuantity).coerceAtLeast(BigDecimal.ZERO)
            if (availableQuantity <= BigDecimal.ZERO) {
                val reason = if (pendingSellQuantity > BigDecimal.ZERO) {
                    TriggerSkipReason.SELL_ALREADY_LOCKED
                } else {
                    TriggerSkipReason.NO_ORDERABLE_QUANTITY
                }
                markTriggersSkipped(firedTriggers, quoteAt, price, reason)
                return
            }

            val orderQuantity = calculateOrderQuantity(availableQuantity, decisions)
            if (orderQuantity <= BigDecimal.ZERO) {
                markTriggersSkipped(firedTriggers, quoteAt, price, TriggerSkipReason.NO_ORDERABLE_QUANTITY)
                return
            }

            val orderGroupId = orderGroupId(groupKey, quoteAt, decisions)
            val created = tryCreateGroupedAutoExitOrderWithRetry(
                position = position,
                quantity = orderQuantity,
                orderGroupId = orderGroupId,
                decisions = decisions,
            )
            if (created) {
                firedTriggers.forEach { it.markTriggered(quoteAt, price) }
            } else {
                firedTriggers.forEach { it.markFailed(quoteAt, price) }
            }
            triggerRepository.saveAll(triggers)
        }
    }

    private fun calculateOrderQuantity(availableQuantity: BigDecimal, decisions: List<TriggerDecision>): BigDecimal {
        val cappedRatio = decisions
            .fold(BigDecimal.ZERO) { acc, decision -> acc.add(decision.exitRatioPercent) }
            .min(HUNDRED)
        return availableQuantity.multiply(cappedRatio)
            .divide(HUNDRED, 8, RoundingMode.DOWN)
    }

    private fun tryCreateGroupedAutoExitOrderWithRetry(
        position: Position,
        quantity: BigDecimal,
        orderGroupId: String,
        decisions: List<TriggerDecision>,
    ): Boolean {
        val accountId = position.account.id ?: return false
        val positionId = position.id ?: return false
        val triggerType = decisions.first().triggerType
        repeat(maxOrderCreateRetries) { attempt ->
            try {
                autoExitOrderPlacementService.createGroupedAutoExitSellOrder(
                    accountId = accountId,
                    ticker = position.ticker,
                    marketType = position.marketType,
                    quantity = quantity,
                    orderGroupId = orderGroupId,
                    triggerAuditInputs = decisions.map {
                        AutoExitTriggerAuditInput(
                            triggerId = it.triggerId,
                            triggerVersion = it.triggerVersion,
                        )
                    },
                )
                return true
            } catch (ex: DataIntegrityViolationException) {
                // Duplicate-key may indicate a concurrent success. Confirm deterministic order existence before success.
                val existing = orderRepository.findByAccountIdAndOrderGroupId(accountId, orderGroupId)
                if (existing != null) {
                    return true
                }
                publishOrderCreateFailure(
                    accountId = accountId,
                    positionId = positionId,
                    triggerType = triggerType,
                    orderGroupId = orderGroupId,
                    quoteAt = decisions.first().quoteAt,
                    reason = "duplicate key without existing order: ${ex.message}",
                )
                return false
            } catch (ex: Exception) {
                if (attempt == maxOrderCreateRetries - 1) {
                    publishOrderCreateFailure(
                        accountId = accountId,
                        positionId = positionId,
                        triggerType = triggerType,
                        orderGroupId = orderGroupId,
                        quoteAt = decisions.first().quoteAt,
                        reason = ex.message ?: ex::class.simpleName.orEmpty(),
                    )
                    return false
                }
            }
        }
        return false
    }

    private fun publishOrderCreateFailure(
        accountId: Long,
        positionId: Long,
        triggerType: TriggerType,
        orderGroupId: String,
        quoteAt: Instant,
        reason: String,
    ) {
        notificationEventPublisher.publishOrderError(
            accountId = accountId,
            orderId = null,
            message = "auto-exit order creation failed. accountId=$accountId, positionId=$positionId, triggerType=${triggerType.name}, orderGroupId=$orderGroupId, quoteAt=$quoteAt, reason=$reason",
        )
    }

    private fun orderGroupId(groupKey: TriggerGroupKey, quoteAt: Instant, decisions: List<TriggerDecision>): String {
        val auditKey = decisions
            .sortedWith(compareBy<TriggerDecision> { it.triggerId }.thenBy { it.triggerVersion })
            .joinToString(",") { "${it.triggerId}@${it.triggerVersion}" }
        val fingerprint = auditKey.hashCode().toUInt().toString(16)
        return "auto-exit:${groupKey.positionId}:${groupKey.triggerType.name}:${quoteAt.toEpochMilli()}:$fingerprint"
    }

    private fun markCandidatesSkipped(
        candidates: List<com.papertrading.api.domain.entity.position.PositionExitTrigger>,
        quoteAt: Instant,
        price: BigDecimal,
        reason: TriggerSkipReason,
    ) {
        markTriggersSkipped(candidates.filter { it.state == TriggerState.ARMED }, quoteAt, price, reason)
    }

    private fun markTriggersSkipped(
        triggers: List<com.papertrading.api.domain.entity.position.PositionExitTrigger>,
        quoteAt: Instant,
        price: BigDecimal,
        reason: TriggerSkipReason,
    ) {
        triggers.forEach { it.markSkipped(quoteAt, price, reason) }
        triggerRepository.saveAll(triggers)
    }

    private fun markTriggersFailed(
        triggers: List<com.papertrading.api.domain.entity.position.PositionExitTrigger>,
        quoteAt: Instant,
        price: BigDecimal,
    ) {
        triggers.forEach { it.markFailed(quoteAt, price) }
        triggerRepository.saveAll(triggers)
    }

    private data class TriggerGroupKey(
        val positionId: Long,
        val triggerType: TriggerType,
    )

    companion object {
        private val HUNDRED = BigDecimal("100.0000")
    }
}
