package com.papertrading.api.domain.entity.order

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.base.BaseAuditEntity
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.enums.OrderType
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
import java.math.RoundingMode
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
    uniqueConstraints = [
        UniqueConstraint(name = "uk_orders_account_idempotency", columnNames = ["account_id", "idempotency_key"]),
    ],
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

    @Column(name = "total_amount", precision = 28, scale = 12)
    var totalAmount: BigDecimal? = null
        private set

    @Column(name = "fee", nullable = false, precision = 20, scale = 4)
    var fee: BigDecimal = BigDecimal.ZERO
        private set

    // 매수 주문 접수 시 잠금된 예수금 — 취소/부분 체결 잔여분 해제 계산에 사용
    @Column(name = "locked_amount", nullable = false, precision = 28, scale = 12)
    var lockedAmount: BigDecimal = BigDecimal.ZERO
        private set

    @Column(name = "idempotency_key", nullable = false, length = 100)
    lateinit var idempotencyKey: String
        private set

    @Column(name = "order_group_id", length = 100)
    var orderGroupId: String? = null
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

    /** 이번 체결 반영, 반환값은 이번 체결 금액(수량 × 체결가, 반올림 없음) */
    fun applyExecution(executedQty: BigDecimal, executedPrice: BigDecimal, executedFee: BigDecimal): BigDecimal {
        check(orderStatus == OrderStatus.PENDING || orderStatus == OrderStatus.PARTIAL) {
            "체결 적용 불가 상태입니다: $orderStatus"
        }
        require(executedQty > BigDecimal.ZERO) { "체결 수량은 0보다 커야 합니다." }
        require(executedPrice > BigDecimal.ZERO) { "체결 가격은 0보다 커야 합니다." }
        require(filledQuantity.add(executedQty) <= quantity) {
            "체결 수량이 주문 잔여 수량을 넘습니다: orderId=$id, executedQty=$executedQty, " +
                "remainingQty=${quantity.subtract(filledQuantity)}"
        }
        val executedAmount = executedQty.multiply(executedPrice)
        val newTotalAmount = (totalAmount ?: BigDecimal.ZERO).add(executedAmount)
        filledQuantity = filledQuantity.add(executedQty)
        totalAmount = newTotalAmount
        avgFilledPrice = newTotalAmount.divide(filledQuantity, AMOUNT_SCALE, RoundingMode.HALF_UP)
        fee = fee.add(executedFee)
        orderStatus = if (filledQuantity >= quantity) OrderStatus.FILLED else OrderStatus.PARTIAL
        return executedAmount
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

    /** 최종 체결 때 계좌에 돌려줄 잠금 잔액 (초기 잠금 - 누적 체결 금액, 0 하한) */
    fun remainingLockedAmount(): BigDecimal {
        val remaining = lockedAmount.subtract(totalAmount ?: BigDecimal.ZERO)
        return remaining.coerceAtLeast(BigDecimal.ZERO)
    }

    /** 매수 주문의 누적 체결 금액이 주문 잠금을 넘은 상태 (잔액 0 하한으로 가려지는 초과 사용) */
    fun isLockedAmountExceeded(): Boolean {
        val executedTotal = totalAmount ?: BigDecimal.ZERO
        return orderSide == OrderSide.BUY && executedTotal > lockedAmount
    }

    fun isCancellable(): Boolean = orderStatus == OrderStatus.PENDING || orderStatus == OrderStatus.PARTIAL

    companion object {
        /** 평균가 표시·수수료 반올림 소수 자리수 (orders 금액 열 scale 4) */
        const val AMOUNT_SCALE = 4

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
            orderGroupId: String? = null,
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
            require(orderGroupId == null || orderGroupId.isNotBlank()) { "orderGroupId는 비어 있을 수 없습니다." }

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
                this.orderGroupId = orderGroupId
                this.expireAt = expireAt
                this.strategyId = strategyId
                this.signalId = signalId
            }
        }
    }
}
