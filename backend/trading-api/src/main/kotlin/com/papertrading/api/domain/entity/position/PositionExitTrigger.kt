package com.papertrading.api.domain.entity.position

import com.papertrading.api.common.exception.BadRequestException
import com.papertrading.api.common.exception.StaleTriggerVersionException
import com.papertrading.api.domain.entity.base.BaseAuditEntity
import com.papertrading.api.domain.enums.TriggerState
import com.papertrading.api.domain.enums.TriggerSkipReason
import com.papertrading.api.domain.enums.TriggerType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

@Entity
@Table(name = "position_exit_triggers")
class PositionExitTrigger protected constructor() : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(name = "position_id", nullable = false, unique = true)
    var positionId: Long = 0
        private set

    @Column(name = "account_id", nullable = false)
    var accountId: Long = 0
        private set

    @Column(name = "ticker", nullable = false, length = 32)
    lateinit var ticker: String
        private set

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = false
        private set

    @Column(name = "stop_loss_percent", precision = 8, scale = 4)
    var stopLossPercent: BigDecimal? = null
        private set

    @Column(name = "take_profit_percent", precision = 8, scale = 4)
    var takeProfitPercent: BigDecimal? = null
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "stop_loss_state", nullable = false, length = 16)
    var stopLossState: TriggerState = TriggerState.ARMED
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "take_profit_state", nullable = false, length = 16)
    var takeProfitState: TriggerState = TriggerState.ARMED
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "triggered_by", length = 16)
    var triggeredBy: TriggerType? = null
        private set

    @Column(name = "triggered_at")
    var triggeredAt: Instant? = null
        private set

    @Column(name = "last_evaluated_at")
    var lastEvaluatedAt: Instant? = null
        private set

    @Column(name = "last_evaluated_price", precision = 18, scale = 4)
    var lastEvaluatedPrice: BigDecimal? = null
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "skip_reason", length = 32)
    var skipReason: TriggerSkipReason? = null
        private set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        private set

    fun upsertPolicy(
        enabled: Boolean,
        stopLossPercent: BigDecimal?,
        takeProfitPercent: BigDecimal?,
        expectedVersion: Long?,
    ) {
        if (expectedVersion != null && expectedVersion != version) {
            throw StaleTriggerVersionException(
                "stale trigger version: expected=$expectedVersion, actual=$version"
            )
        }

        val normalizedStopLoss = normalizePercent(stopLossPercent)
        val normalizedTakeProfit = normalizePercent(takeProfitPercent)
        validatePolicy(enabled, normalizedStopLoss, normalizedTakeProfit)

        this.enabled = enabled
        this.stopLossPercent = normalizedStopLoss
        this.takeProfitPercent = normalizedTakeProfit
        rearmIfNeeded()
    }

    fun markTriggered(type: TriggerType, now: Instant, quotePrice: BigDecimal) {
        require(enabled) { "disabled trigger cannot be triggered" }
        require(triggeredBy == null) { "trigger already completed: triggeredBy=$triggeredBy" }

        if (type == TriggerType.STOP_LOSS) {
            stopLossState = TriggerState.TRIGGERED
            takeProfitState = TriggerState.CANCELED
        } else {
            takeProfitState = TriggerState.TRIGGERED
            stopLossState = TriggerState.CANCELED
        }
        triggeredBy = type
        triggeredAt = now
        lastEvaluatedAt = now
        lastEvaluatedPrice = quotePrice
        skipReason = null
    }

    fun markFailed(type: TriggerType, now: Instant, quotePrice: BigDecimal) {
        require(enabled) { "disabled trigger cannot fail" }
        require(triggeredBy == null) { "trigger already completed: triggeredBy=$triggeredBy" }

        if (type == TriggerType.STOP_LOSS) {
            stopLossState = TriggerState.FAILED
        } else {
            takeProfitState = TriggerState.FAILED
        }
        lastEvaluatedAt = now
        lastEvaluatedPrice = quotePrice
        skipReason = null
    }

    fun markSkipped(type: TriggerType, now: Instant, quotePrice: BigDecimal, reason: TriggerSkipReason) {
        require(enabled) { "disabled trigger cannot skip" }
        require(triggeredBy == null) { "trigger already completed: triggeredBy=$triggeredBy" }

        if (type == TriggerType.STOP_LOSS) {
            stopLossState = TriggerState.SKIPPED
            takeProfitState = TriggerState.CANCELED
        } else {
            takeProfitState = TriggerState.SKIPPED
            stopLossState = TriggerState.CANCELED
        }
        triggeredBy = type
        triggeredAt = now
        lastEvaluatedAt = now
        lastEvaluatedPrice = quotePrice
        skipReason = reason
    }

    private fun rearmIfNeeded() {
        if (!enabled) {
            stopLossState = TriggerState.CANCELED
            takeProfitState = TriggerState.CANCELED
            triggeredBy = null
            triggeredAt = null
            skipReason = null
            return
        }
        stopLossState = if (stopLossPercent != null) TriggerState.ARMED else TriggerState.CANCELED
        takeProfitState = if (takeProfitPercent != null) TriggerState.ARMED else TriggerState.CANCELED
        triggeredBy = null
        triggeredAt = null
        skipReason = null
    }

    private fun validatePolicy(
        enabled: Boolean,
        stopLossPercent: BigDecimal?,
        takeProfitPercent: BigDecimal?,
    ) {
        validatePercentRange(stopLossPercent)
        validatePercentRange(takeProfitPercent)
        if (enabled && stopLossPercent == null && takeProfitPercent == null) {
            throw BadRequestException("EXIT_TRIGGER_PERCENT_REQUIRED", "enabled trigger needs at least one percent")
        }
    }

    private fun validatePercentRange(value: BigDecimal?) {
        if (value != null && (value <= BigDecimal.ZERO || value >= BigDecimal("100"))) {
            throw BadRequestException("INVALID_PERCENT_RANGE", "percent must be in (0,100)")
        }
    }

    private fun normalizePercent(value: BigDecimal?): BigDecimal? =
        value?.setScale(4, RoundingMode.HALF_UP)

    companion object {
        fun create(
            positionId: Long,
            accountId: Long,
            ticker: String,
            enabled: Boolean,
            stopLossPercent: BigDecimal?,
            takeProfitPercent: BigDecimal?,
        ): PositionExitTrigger =
            PositionExitTrigger().apply {
                require(positionId > 0) { "positionId must be positive" }
                require(accountId > 0) { "accountId must be positive" }
                require(ticker.isNotBlank()) { "ticker must not be blank" }
                this.positionId = positionId
                this.accountId = accountId
                this.ticker = ticker.uppercase()
                this.enabled = false
                this.stopLossPercent = null
                this.takeProfitPercent = null
                this.upsertPolicy(
                    enabled = enabled,
                    stopLossPercent = stopLossPercent,
                    takeProfitPercent = takeProfitPercent,
                    expectedVersion = null,
                )
            }
    }
}
