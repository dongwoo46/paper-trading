package com.papertrading.api.domain.entity.order

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.SignalCategory
import com.papertrading.api.domain.enums.SignalIndicator
import com.papertrading.api.domain.entity.base.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal

/**
 * 주문 신호 (Python research-service → trading-api 진입점)
 * isProcessed=false 인 신호만 Order 변환 대상. 멱등 처리(idempotencyKey UNIQUE).
 * conditionSnapshot: 신호 생성 시점 지표 스냅샷(JSONB) — 사후 분석용.
 */
@Entity
@Table(
    name = "order_signals",
    uniqueConstraints = [UniqueConstraint(name = "uk_order_signals_idempotency", columnNames = ["idempotency_key"])]
)
class OrderSignal protected constructor() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account
        private set

    @Column(name = "strategy_id")
    var strategyId: Long? = null
        private set

    @Column(name = "ticker", nullable = false, length = 20)
    lateinit var ticker: String
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_side", nullable = false, length = 10)
    lateinit var signalSide: OrderSide
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_category", length = 30)
    var signalCategory: SignalCategory? = null
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_indicator", length = 20)
    var signalIndicator: SignalIndicator? = null
        private set

    @Column(name = "signal_threshold", precision = 20, scale = 8)
    var signalThreshold: BigDecimal? = null
        private set

    @Column(name = "signal_actual_value", precision = 20, scale = 8)
    var signalActualValue: BigDecimal? = null
        private set

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "condition_snapshot", columnDefinition = "jsonb")
    var conditionSnapshot: String? = null
        private set

    @Column(name = "quantity", nullable = false, precision = 20, scale = 8)
    lateinit var quantity: BigDecimal
        private set

    @Column(name = "limit_price", precision = 20, scale = 4)
    var limitPrice: BigDecimal? = null
        private set

    @Column(name = "is_processed", nullable = false)
    var isProcessed: Boolean = false
        private set

    @Column(name = "idempotency_key", nullable = false, length = 100)
    lateinit var idempotencyKey: String
        private set

    fun markAsProcessed() {
        check(!isProcessed) { "이미 처리된 신호입니다." }
        isProcessed = true
    }

    companion object {
        fun create(
            account: Account,
            ticker: String,
            signalSide: OrderSide,
            quantity: BigDecimal,
            idempotencyKey: String,
            strategyId: Long? = null,
            signalCategory: SignalCategory? = null,
            signalIndicator: SignalIndicator? = null,
            signalThreshold: BigDecimal? = null,
            signalActualValue: BigDecimal? = null,
            conditionSnapshot: String? = null,
            limitPrice: BigDecimal? = null
        ): OrderSignal {
            require(ticker.isNotBlank()) { "종목코드는 비어 있을 수 없습니다." }
            require(quantity > BigDecimal.ZERO) { "수량은 0보다 커야 합니다." }
            require(idempotencyKey.isNotBlank()) { "idempotencyKey는 비어 있을 수 없습니다." }
            require(limitPrice == null || limitPrice > BigDecimal.ZERO) { "지정가는 0보다 커야 합니다." }

            return OrderSignal().apply {
                this.account = account
                this.ticker = ticker.uppercase()
                this.signalSide = signalSide
                this.quantity = quantity
                this.idempotencyKey = idempotencyKey
                this.strategyId = strategyId
                this.signalCategory = signalCategory
                this.signalIndicator = signalIndicator
                this.signalThreshold = signalThreshold
                this.signalActualValue = signalActualValue
                this.conditionSnapshot = conditionSnapshot
                this.limitPrice = limitPrice
            }
        }
    }
}