package com.papertrading.api.domain.entity.order

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.enums.OrderType
import com.papertrading.api.domain.entity.base.BaseAuditEntity
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
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.Instant

/**
 * 주문 (Aggregate Root)
 * PENDING → PARTIAL → FILLED / CANCELLED 상태 머신.
 * version 낙관적 락으로 동시 체결 충돌 방지.
 * externalOrderId: KIS 연동 시 외부 주문번호 — 접수 후 한 번만 설정 가능.
 */
@Entity
@Table(
    name = "orders",
    uniqueConstraints = [UniqueConstraint(name = "uk_orders_account_idempotency", columnNames = ["account_id", "idempotency_key"])]
)
class Order protected constructor() : BaseAuditEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account
        private set

    @Column(name = "ticker", nullable = false, length = 20)
    lateinit var ticker: String
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", nullable = false, length = 20)
    lateinit var marketType: MarketType
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    lateinit var orderType: OrderType
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "order_side", nullable = false, length = 10)
    lateinit var orderSide: OrderSide
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "order_condition", nullable = false, length = 10)
    lateinit var orderCondition: OrderCondition
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 20)
    var orderStatus: OrderStatus = OrderStatus.PENDING
        private set

    @Column(name = "quantity", nullable = false, precision = 20, scale = 8)
    lateinit var quantity: BigDecimal
        private set

    @Column(name = "filled_quantity", nullable = false, precision = 20, scale = 8)
    var filledQuantity: BigDecimal = BigDecimal.ZERO
        private set

    @Column(name = "limit_price", precision = 20, scale = 4)
    var limitPrice: BigDecimal? = null
        private set

    @Column(name = "avg_filled_price", precision = 20, scale = 4)
    var avgFilledPrice: BigDecimal? = null
        private set

    @Column(name = "total_amount", precision = 20, scale = 4)
    var totalAmount: BigDecimal? = null
        private set

    @Column(name = "fee", nullable = false, precision = 20, scale = 4)
    var fee: BigDecimal = BigDecimal.ZERO
        private set

    // 매수 주문 접수 시 잠금된 예수금 — 취소/부분 체결 잔여분 해제 계산에 사용
    @Column(name = "locked_amount", nullable = false, precision = 20, scale = 4)
    var lockedAmount: BigDecimal = BigDecimal.ZERO
        private set

    @Column(name = "idempotency_key", nullable = false, length = 100)
    lateinit var idempotencyKey: String
        private set

    @Column(name = "external_order_id", length = 100)
    var externalOrderId: String? = null
        private set

    @Column(name = "expire_at")
    var expireAt: Instant? = null
        private set

    @Column(name = "strategy_id")
    var strategyId: Long? = null
        private set

    @Column(name = "signal_id")
    var signalId: Long? = null
        private set

    @Version
    @Column(name = "version", nullable = false)
    var version: Int = 0

    fun applyExecution(executedQty: BigDecimal, newAvgPrice: BigDecimal, executedFee: BigDecimal) {
        check(orderStatus == OrderStatus.PENDING || orderStatus == OrderStatus.PARTIAL) {
            "체결 적용 불가 상태입니다: $orderStatus"
        }
        filledQuantity = filledQuantity.add(executedQty)
        avgFilledPrice = newAvgPrice
        totalAmount = (totalAmount ?: BigDecimal.ZERO).add(executedQty.multiply(newAvgPrice))
        fee = fee.add(executedFee)
        orderStatus = if (filledQuantity >= quantity) OrderStatus.FILLED else OrderStatus.PARTIAL
    }

    fun cancel() {
        check(orderStatus == OrderStatus.PENDING || orderStatus == OrderStatus.PARTIAL) {
            "취소 불가 상태입니다: $orderStatus"
        }
        orderStatus = OrderStatus.CANCELLED
    }

    fun assignExternalOrderId(externalId: String) {
        check(externalOrderId == null) { "외부 주문번호는 한 번만 설정 가능합니다." }
        externalOrderId = externalId
    }

    /** 취소 시 계좌에 반환할 잠금 해제 금액 (초기 잠금 - 실체결 금액) */
    fun remainingLockedAmount(): BigDecimal =
        lockedAmount.subtract(totalAmount ?: BigDecimal.ZERO).coerceAtLeast(BigDecimal.ZERO)

    fun isCancellable(): Boolean =
        orderStatus == OrderStatus.PENDING || orderStatus == OrderStatus.PARTIAL

    companion object {
        fun create(
            account: Account,
            ticker: String,
            marketType: MarketType,
            orderType: OrderType,
            orderSide: OrderSide,
            orderCondition: OrderCondition,
            quantity: BigDecimal,
            limitPrice: BigDecimal? = null,
            lockedAmount: BigDecimal = BigDecimal.ZERO,
            idempotencyKey: String,
            expireAt: Instant? = null,
            strategyId: Long? = null,
            signalId: Long? = null
        ): Order {
            require(ticker.isNotBlank()) { "종목코드는 비어 있을 수 없습니다." }
            require(quantity > BigDecimal.ZERO) { "주문 수량은 0보다 커야 합니다." }
            require(orderType != OrderType.LIMIT || limitPrice != null) { "지정가 주문은 가격이 필요합니다." }
            require(limitPrice == null || limitPrice > BigDecimal.ZERO) { "주문 가격은 0보다 커야 합니다." }
            require(lockedAmount >= BigDecimal.ZERO) { "잠금 금액은 0 이상이어야 합니다." }
            require(idempotencyKey.isNotBlank()) { "idempotencyKey는 비어 있을 수 없습니다." }

            return Order().apply {
                this.account = account
                this.ticker = ticker.uppercase()
                this.marketType = marketType
                this.orderType = orderType
                this.orderSide = orderSide
                this.orderCondition = orderCondition
                this.quantity = quantity
                this.limitPrice = limitPrice
                this.lockedAmount = lockedAmount
                this.idempotencyKey = idempotencyKey
                this.expireAt = expireAt
                this.strategyId = strategyId
                this.signalId = signalId
            }
        }
    }
}