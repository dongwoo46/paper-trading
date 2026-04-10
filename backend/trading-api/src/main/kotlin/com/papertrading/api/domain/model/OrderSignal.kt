package com.papertrading.api.domain.model

import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.SignalCategory
import com.papertrading.api.domain.enums.SignalIndicator
import com.papertrading.api.domain.model.base.BaseTimeEntity
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
 * Python 전략이 생성한 매매 신호를 수신·검증 후 실제 Order로 전환한다.
 * isProcessed=false인 신호만 Order 변환 대상. 멱등 처리(idempotencyKey UNIQUE).
 * conditionSnapshot: 신호 생성 시점의 지표 스냅샷(JSONB) — 사후 분석용.
 */
@Entity
@Table(
    name = "order_signals",
    uniqueConstraints = [UniqueConstraint(name = "uk_order_signals_idempotency", columnNames = ["idempotency_key"])]
)
class OrderSignal(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    var account: Account? = null,

    @Column(name = "strategy_id")
    var strategyId: Long? = null,

    @Column(name = "ticker", nullable = false, length = 20)
    var ticker: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_side", nullable = false, length = 10)
    var signalSide: OrderSide? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_category", length = 30)
    var signalCategory: SignalCategory? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_indicator", length = 20)
    var signalIndicator: SignalIndicator? = null,

    @Column(name = "signal_threshold", precision = 20, scale = 8)
    var signalThreshold: BigDecimal? = null,

    @Column(name = "signal_actual_value", precision = 20, scale = 8)
    var signalActualValue: BigDecimal? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "condition_snapshot", columnDefinition = "jsonb")
    var conditionSnapshot: String? = null,

    @Column(name = "quantity", nullable = false, precision = 20, scale = 8)
    var quantity: BigDecimal = BigDecimal.ZERO,

    @Column(name = "limit_price", precision = 20, scale = 4)
    var limitPrice: BigDecimal? = null,

    @Column(name = "is_processed", nullable = false)
    var isProcessed: Boolean = false,

    @Column(name = "idempotency_key", nullable = false, length = 100)
    var idempotencyKey: String? = null
) : BaseTimeEntity() {
    fun markAsProcessed() {
        isProcessed = true
    }
}
