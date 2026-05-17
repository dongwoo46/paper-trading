package com.papertrading.api.application.position

import com.papertrading.api.application.notification.SlackNotificationEventPublisher
import com.papertrading.api.application.order.OrderCommandService
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.enums.TriggerSkipReason
import com.papertrading.api.domain.enums.TriggerType
import com.papertrading.api.infrastructure.persistence.OrderRepository
import com.papertrading.api.infrastructure.persistence.PositionExitTriggerRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

// 시세가 들어왔을 때 포지션의 청산 트리거를 평가하고, 필요하면 자동 청산 주문 실행을 지시
@Service
class PositionExitTriggerOrchestrator(
    private val positionRepository: PositionRepository,
    private val triggerRepository: PositionExitTriggerRepository,
    private val evaluator: PositionExitTriggerEvaluator,
    private val orderCommandService: OrderCommandService,
    private val orderRepository: OrderRepository,
    private val notificationEventPublisher: SlackNotificationEventPublisher,
) {
    private val positionLocks = ConcurrentHashMap<Long, Any>()
    private val maxOrderCreateRetries = 3

    @Transactional
    fun onQuote(ticker: String, price: BigDecimal, quoteAt: Instant) {
        val positions = positionRepository.findByTickerAndQuantityGreaterThan(ticker, BigDecimal.ZERO)
        positions.forEach { position ->
            val positionId = position.id ?: return@forEach
            synchronized(positionLocks.computeIfAbsent(positionId) { Any() }) {
                if (position.quantity <= BigDecimal.ZERO) return@synchronized
                val trigger = triggerRepository.findByPositionIdForUpdate(positionId) ?: return@synchronized
                val decision = evaluator.evaluate(position, trigger, price, quoteAt) ?: return@synchronized
                val lockedPosition = positionRepository.findByIdWithLock(positionId).orElse(null)
                val skipReason = skipReasonFor(lockedPosition)
                if (skipReason != null) {
                    trigger.markSkipped(decision.type, quoteAt, price, skipReason)
                    triggerRepository.save(trigger)
                    return@synchronized
                }

                val orderablePosition = lockedPosition ?: return@synchronized
                val created = tryCreateAutoExitOrderWithRetry(orderablePosition, trigger.version, decision.type, quoteAt)
                if (created) {
                    trigger.markTriggered(decision.type, quoteAt, price)
                } else {
                    trigger.markFailed(decision.type, quoteAt, price)
                }
                triggerRepository.save(trigger)
            }
        }
    }

    private fun skipReasonFor(position: Position?): TriggerSkipReason? {
        if (position == null || position.quantity <= BigDecimal.ZERO) {
            return TriggerSkipReason.POSITION_CLOSED
        }
        if (position.orderableQuantity <= BigDecimal.ZERO && position.lockedQuantity > BigDecimal.ZERO) {
            return TriggerSkipReason.SELL_ALREADY_LOCKED
        }
        if (position.orderableQuantity <= BigDecimal.ZERO) {
            return TriggerSkipReason.NO_ORDERABLE_QUANTITY
        }
        return null
    }

    private fun tryCreateAutoExitOrderWithRetry(
        position: Position,
        triggerEntityVersion: Long,
        triggerType: TriggerType,
        quoteAt: Instant,
    ): Boolean {
        val accountId = position.account.id ?: return false
        val positionId = position.id ?: return false
        val idempotencyKey = autoExitIdempotencyKey(positionId, triggerEntityVersion, triggerType)
        repeat(maxOrderCreateRetries) { attempt ->
            try {
                orderCommandService.createAutoExitSellOrder(position, triggerEntityVersion, triggerType)
                return true
            } catch (ex: DataIntegrityViolationException) {
                // Duplicate-key may indicate a concurrent success. Confirm deterministic order existence before success.
                val existing = orderRepository.findByAccountIdAndIdempotencyKey(accountId, idempotencyKey)
                if (existing != null) {
                    return true
                }
                publishOrderCreateFailure(
                    accountId = accountId,
                    positionId = positionId,
                    triggerType = triggerType,
                    idempotencyKey = idempotencyKey,
                    quoteAt = quoteAt,
                    reason = "duplicate key without existing order: ${ex.message}",
                )
                return false
            } catch (ex: Exception) {
                if (attempt == maxOrderCreateRetries - 1) {
                    publishOrderCreateFailure(
                        accountId = accountId,
                        positionId = positionId,
                        triggerType = triggerType,
                        idempotencyKey = idempotencyKey,
                        quoteAt = quoteAt,
                        reason = ex.message ?: ex::class.simpleName.orEmpty(),
                    )
                    return false
                }
            }
        }
        return false
    }

    private fun autoExitIdempotencyKey(positionId: Long, triggerEntityVersion: Long, triggerType: TriggerType): String =
        "auto-exit:$positionId:$triggerEntityVersion:${triggerType.name}"

    private fun publishOrderCreateFailure(
        accountId: Long,
        positionId: Long,
        triggerType: TriggerType,
        idempotencyKey: String,
        quoteAt: Instant,
        reason: String,
    ) {
        notificationEventPublisher.publishOrderError(
            accountId = accountId,
            orderId = null,
            message = "auto-exit order creation failed. accountId=$accountId, positionId=$positionId, triggerType=${triggerType.name}, idempotencyKey=$idempotencyKey, quoteAt=$quoteAt, reason=$reason",
        )
    }
}
