package com.papertrading.api.domain.model

import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.enums.OrderType
import com.papertrading.api.domain.model.base.BaseAuditEntity
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

@Entity
@Table(
    name = "orders",
    uniqueConstraints = [UniqueConstraint(name = "uk_orders_account_idempotency", columnNames = ["account_id", "idempotency_key"])]
)
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    var account: Account? = null,

    @Column(name = "ticker", nullable = false, length = 20)
    var ticker: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", nullable = false, length = 20)
    var marketType: MarketType? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    var orderType: OrderType? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "order_side", nullable = false, length = 10)
    var orderSide: OrderSide? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "order_condition", nullable = false, length = 10)
    var orderCondition: OrderCondition? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 20)
    var orderStatus: OrderStatus = OrderStatus.PENDING,

    @Column(name = "quantity", nullable = false, precision = 20, scale = 8)
    var quantity: BigDecimal = BigDecimal.ZERO,

    @Column(name = "filled_quantity", nullable = false, precision = 20, scale = 8)
    var filledQuantity: BigDecimal = BigDecimal.ZERO,

    @Column(name = "limit_price", precision = 20, scale = 4)
    var limitPrice: BigDecimal? = null,

    @Column(name = "avg_filled_price", precision = 20, scale = 4)
    var avgFilledPrice: BigDecimal? = null,

    @Column(name = "total_amount", precision = 20, scale = 4)
    var totalAmount: BigDecimal? = null,

    @Column(name = "fee", nullable = false, precision = 20, scale = 4)
    var fee: BigDecimal = BigDecimal.ZERO,

    @Column(name = "idempotency_key", nullable = false, length = 100)
    var idempotencyKey: String? = null,

    @Column(name = "external_order_id", length = 100)
    var externalOrderId: String? = null,

    @Column(name = "expire_at")
    var expireAt: Instant? = null,

    @Column(name = "strategy_id")
    var strategyId: Long? = null,

    @Column(name = "signal_id")
    var signalId: Long? = null,

    @Version
    @Column(name = "version", nullable = false)
    var version: Int = 0
) : BaseAuditEntity() {

    fun updateStatus(newStatus: OrderStatus) {
        orderStatus = newStatus
    }

    fun applyExecution(executedQty: BigDecimal, newAvgPrice: BigDecimal, executedFee: BigDecimal) {
        filledQuantity = filledQuantity.add(executedQty)
        avgFilledPrice = newAvgPrice
        totalAmount = (totalAmount ?: BigDecimal.ZERO).add(executedQty.multiply(newAvgPrice))
        fee = fee.add(executedFee)

        orderStatus = if (filledQuantity >= quantity) {
            OrderStatus.FILLED
        } else {
            OrderStatus.PARTIAL
        }
    }
}
