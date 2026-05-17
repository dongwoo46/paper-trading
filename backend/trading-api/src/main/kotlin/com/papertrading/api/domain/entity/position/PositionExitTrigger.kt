package com.papertrading.api.domain.entity.position

import com.papertrading.api.common.exception.BadRequestException
import com.papertrading.api.common.exception.ConflictException
import com.papertrading.api.common.exception.StaleTriggerVersionException
import com.papertrading.api.domain.entity.base.BaseAuditEntity
import com.papertrading.api.domain.enums.PriceBasisPolicy
import com.papertrading.api.domain.enums.TriggerSkipReason
import com.papertrading.api.domain.enums.TriggerState
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

    @Column(name = "position_id", nullable = false)
    var positionId: Long = 0
        private set

    @Column(name = "account_id", nullable = false)
    var accountId: Long = 0
        private set

    @Column(name = "ticker", nullable = false, length = 32)
    lateinit var ticker: String
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 16)
    lateinit var triggerType: TriggerType
        private set

    @Column(name = "trigger_percent", precision = 8, scale = 4)
    var triggerPercent: BigDecimal? = null
        private set

    @Column(name = "trigger_price", precision = 20, scale = 4)
    var triggerPrice: BigDecimal? = null
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "price_basis_policy", nullable = false, length = 32)
    lateinit var priceBasisPolicy: PriceBasisPolicy
        private set

    @Column(name = "exit_ratio_percent", nullable = false, precision = 8, scale = 4)
    var exitRatioPercent: BigDecimal = DEFAULT_EXIT_RATIO_PERCENT
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 16)
    var state: TriggerState = TriggerState.ARMED
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "skip_reason", length = 32)
    var skipReason: TriggerSkipReason? = null
        private set

    @Column(name = "triggered_at")
    var triggeredAt: Instant? = null
        private set

    @Column(name = "last_evaluated_at")
    var lastEvaluatedAt: Instant? = null
        private set

    @Column(name = "last_evaluated_price", precision = 20, scale = 4)
    var lastEvaluatedPrice: BigDecimal? = null
        private set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        private set

    fun update(
        triggerPercent: BigDecimal?,
        triggerPrice: BigDecimal?,
        priceBasisPolicy: PriceBasisPolicy,
        exitRatioPercent: BigDecimal?,
        expectedVersion: Long?,
    ) {
        assertExpectedVersion(expectedVersion)
        requireState(TriggerState.ARMED, "only ARMED trigger can be updated")
        applyPolicy(triggerPercent, triggerPrice, priceBasisPolicy, exitRatioPercent)
        skipReason = null
    }

    fun cancel(expectedVersion: Long? = null) {
        assertExpectedVersion(expectedVersion)
        requireState(TriggerState.ARMED, "only ARMED trigger can be canceled")
        state = TriggerState.CANCELED
        skipReason = null
    }

    fun recordEvaluation(now: Instant, quotePrice: BigDecimal) {
        requireState(TriggerState.ARMED, "only ARMED trigger can be evaluated")
        lastEvaluatedAt = now
        lastEvaluatedPrice = normalizePrice(quotePrice)
    }

    fun markTriggered(now: Instant, quotePrice: BigDecimal) {
        requireState(TriggerState.ARMED, "only ARMED trigger can be triggered")
        state = TriggerState.TRIGGERED
        triggeredAt = now
        lastEvaluatedAt = now
        lastEvaluatedPrice = normalizePrice(quotePrice)
        skipReason = null
    }

    fun markFailed(now: Instant, quotePrice: BigDecimal) {
        requireState(TriggerState.ARMED, "only ARMED trigger can fail")
        state = TriggerState.FAILED
        lastEvaluatedAt = now
        lastEvaluatedPrice = normalizePrice(quotePrice)
        skipReason = null
    }

    fun markSkipped(now: Instant, quotePrice: BigDecimal, reason: TriggerSkipReason) {
        requireState(TriggerState.ARMED, "only ARMED trigger can be skipped")
        state = TriggerState.SKIPPED
        triggeredAt = now
        lastEvaluatedAt = now
        lastEvaluatedPrice = normalizePrice(quotePrice)
        skipReason = reason
    }

    private fun applyPolicy(
        triggerPercent: BigDecimal?,
        triggerPrice: BigDecimal?,
        priceBasisPolicy: PriceBasisPolicy,
        exitRatioPercent: BigDecimal?,
    ) {
        val normalizedPercent = normalizePercent(triggerPercent)
        val normalizedPrice = normalizePrice(triggerPrice)
        val normalizedExitRatio = normalizeExitRatio(exitRatioPercent ?: DEFAULT_EXIT_RATIO_PERCENT)
        validatePolicy(normalizedPercent, normalizedPrice, priceBasisPolicy)

        this.triggerPercent = normalizedPercent
        this.triggerPrice = normalizedPrice
        this.priceBasisPolicy = priceBasisPolicy
        this.exitRatioPercent = normalizedExitRatio
    }

    private fun validatePolicy(
        triggerPercent: BigDecimal?,
        triggerPrice: BigDecimal?,
        priceBasisPolicy: PriceBasisPolicy,
    ) {
        if (triggerPercent == null && triggerPrice == null) {
            throw BadRequestException("EXIT_TRIGGER_CONDITION_REQUIRED", "triggerPercent or triggerPrice is required")
        }
        if (triggerPercent != null && (triggerPercent <= BigDecimal.ZERO || triggerPercent >= HUNDRED)) {
            throw BadRequestException("INVALID_PERCENT_RANGE", "percent must be in (0,100)")
        }
        if (triggerPrice != null && triggerPrice <= BigDecimal.ZERO) {
            throw BadRequestException("INVALID_TRIGGER_PRICE", "triggerPrice must be positive")
        }
        when (priceBasisPolicy) {
            PriceBasisPolicy.FIXED_PRICE -> {
                if (triggerPrice == null) {
                    throw BadRequestException("TRIGGER_PRICE_REQUIRED", "FIXED_PRICE requires triggerPrice")
                }
            }
            PriceBasisPolicy.AVG_PRICE_AT_CREATION -> {
                if (triggerPercent == null || triggerPrice == null) {
                    throw BadRequestException(
                        "TRIGGER_PERCENT_AND_PRICE_REQUIRED",
                        "AVG_PRICE_AT_CREATION requires triggerPercent and computed triggerPrice",
                    )
                }
            }
            PriceBasisPolicy.FOLLOW_AVG_PRICE -> {
                if (triggerPercent == null) {
                    throw BadRequestException("TRIGGER_PERCENT_REQUIRED", "FOLLOW_AVG_PRICE requires triggerPercent")
                }
            }
        }
    }

    private fun normalizeExitRatio(value: BigDecimal): BigDecimal {
        val normalized = normalizePercent(value) ?: DEFAULT_EXIT_RATIO_PERCENT
        if (normalized <= BigDecimal.ZERO || normalized > HUNDRED) {
            throw BadRequestException("INVALID_EXIT_RATIO_RANGE", "exitRatioPercent must be in (0,100]")
        }
        return normalized
    }

    private fun assertExpectedVersion(expectedVersion: Long?) {
        if (expectedVersion != null && expectedVersion != version) {
            throw StaleTriggerVersionException(
                "stale trigger version: expected=$expectedVersion, actual=$version",
            )
        }
    }

    private fun requireState(required: TriggerState, message: String) {
        if (state != required) {
            throw ConflictException("INVALID_TRIGGER_STATE", "$message. state=$state")
        }
    }

    private fun normalizePercent(value: BigDecimal?): BigDecimal? =
        value?.setScale(4, RoundingMode.HALF_UP)

    private fun normalizePrice(value: BigDecimal?): BigDecimal? =
        value?.setScale(4, RoundingMode.HALF_UP)

    companion object {
        val DEFAULT_EXIT_RATIO_PERCENT: BigDecimal = BigDecimal("100.0000")
        private val HUNDRED = BigDecimal("100.0000")

        fun create(
            positionId: Long,
            accountId: Long,
            ticker: String,
            triggerType: TriggerType,
            triggerPercent: BigDecimal?,
            triggerPrice: BigDecimal?,
            priceBasisPolicy: PriceBasisPolicy,
            exitRatioPercent: BigDecimal? = null,
        ): PositionExitTrigger =
            PositionExitTrigger().apply {
                require(positionId > 0) { "positionId must be positive" }
                require(accountId > 0) { "accountId must be positive" }
                require(ticker.isNotBlank()) { "ticker must not be blank" }
                this.positionId = positionId
                this.accountId = accountId
                this.ticker = ticker.uppercase()
                this.triggerType = triggerType
                this.state = TriggerState.ARMED
                applyPolicy(triggerPercent, triggerPrice, priceBasisPolicy, exitRatioPercent)
            }
    }
}
