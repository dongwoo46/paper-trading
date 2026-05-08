package com.papertrading.api.domain.position

import com.papertrading.api.domain.entity.base.BaseAuditEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "position_exit_triggers")
class PositionExitTrigger protected constructor() : BaseAuditEntity() {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
    @Column(name = "position_id", nullable = false, unique = true)
    var positionId: Long = 0
    @Column(name = "account_id", nullable = false)
    var accountId: Long = 0
    @Column(name = "ticker", nullable = false, length = 32)
    lateinit var ticker: String
    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = false
    @Column(name = "stop_loss_percent", precision = 8, scale = 4)
    var stopLossPercent: BigDecimal? = null
    @Column(name = "take_profit_percent", precision = 8, scale = 4)
    var takeProfitPercent: BigDecimal? = null
    @Enumerated(EnumType.STRING)
    @Column(name = "stop_loss_state", nullable = false, length = 16)
    var stopLossState: TriggerState = TriggerState.ARMED
    @Enumerated(EnumType.STRING)
    @Column(name = "take_profit_state", nullable = false, length = 16)
    var takeProfitState: TriggerState = TriggerState.ARMED
    @Enumerated(EnumType.STRING)
    @Column(name = "triggered_by", length = 16)
    var triggeredBy: TriggerType? = null
    @Column(name = "triggered_at")
    var triggeredAt: Instant? = null
    @Column(name = "last_evaluated_at")
    var lastEvaluatedAt: Instant? = null
    @Column(name = "last_evaluated_price", precision = 18, scale = 4)
    var lastEvaluatedPrice: BigDecimal? = null
    @Column(name = "trigger_version", nullable = false)
    var triggerVersion: Long = 1
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0

    fun validate() {
        fun checkPercent(v: BigDecimal?) {
            if (v != null) require(v > BigDecimal.ZERO && v < BigDecimal("100")) { "percent must be in (0,100)" }
        }
        checkPercent(stopLossPercent)
        checkPercent(takeProfitPercent)
        if (enabled) require(stopLossPercent != null || takeProfitPercent != null) { "enabled trigger needs at least one percent" }
    }

    fun markTriggered(type: TriggerType, now: Instant, quotePrice: BigDecimal) {
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
    }

    fun markFailed(type: TriggerType, now: Instant, quotePrice: BigDecimal) {
        if (type == TriggerType.STOP_LOSS) {
            stopLossState = TriggerState.FAILED
        } else {
            takeProfitState = TriggerState.FAILED
        }
        lastEvaluatedAt = now
        lastEvaluatedPrice = quotePrice
    }

    companion object {
        fun create(positionId: Long, accountId: Long, ticker: String, enabled: Boolean, stopLossPercent: BigDecimal?, takeProfitPercent: BigDecimal?): PositionExitTrigger =
            PositionExitTrigger().apply {
                this.positionId = positionId
                this.accountId = accountId
                this.ticker = ticker
                this.enabled = enabled
                this.stopLossPercent = stopLossPercent
                this.takeProfitPercent = takeProfitPercent
                validate()
            }
    }
}
