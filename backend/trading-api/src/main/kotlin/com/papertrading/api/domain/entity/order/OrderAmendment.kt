package com.papertrading.api.domain.entity.order

import com.papertrading.api.domain.entity.base.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal

/**
 * 주문 정정·취소 이력 (불변 감사 레코드)
 * 생성 후 수정 없음. amendmentType: MODIFY(정정) | CANCEL(취소)
 */
@Entity
@Table(name = "order_amendments")
class OrderAmendment protected constructor() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    lateinit var order: Order
        private set

    @Column(name = "amendment_type", nullable = false, length = 20)
    lateinit var amendmentType: String
        private set

    @Column(name = "before_quantity", precision = 20, scale = 8)
    var beforeQuantity: BigDecimal? = null
        private set

    @Column(name = "after_quantity", precision = 20, scale = 8)
    var afterQuantity: BigDecimal? = null
        private set

    @Column(name = "before_limit_price", precision = 20, scale = 4)
    var beforeLimitPrice: BigDecimal? = null
        private set

    @Column(name = "after_limit_price", precision = 20, scale = 4)
    var afterLimitPrice: BigDecimal? = null
        private set

    @Column(name = "reason", length = 500)
    var reason: String? = null
        private set

    companion object {
        private val VALID_TYPES = setOf("MODIFY", "CANCEL")

        fun modify(
            order: Order,
            beforeQuantity: BigDecimal? = null,
            afterQuantity: BigDecimal? = null,
            beforeLimitPrice: BigDecimal? = null,
            afterLimitPrice: BigDecimal? = null,
            reason: String? = null
        ): OrderAmendment = create(
            order = order,
            amendmentType = "MODIFY",
            beforeQuantity = beforeQuantity,
            afterQuantity = afterQuantity,
            beforeLimitPrice = beforeLimitPrice,
            afterLimitPrice = afterLimitPrice,
            reason = reason
        )

        fun cancel(order: Order, reason: String? = null): OrderAmendment = create(
            order = order,
            amendmentType = "CANCEL",
            beforeQuantity = order.quantity,
            reason = reason
        )

        fun create(
            order: Order,
            amendmentType: String,
            beforeQuantity: BigDecimal? = null,
            afterQuantity: BigDecimal? = null,
            beforeLimitPrice: BigDecimal? = null,
            afterLimitPrice: BigDecimal? = null,
            reason: String? = null
        ): OrderAmendment {
            require(amendmentType in VALID_TYPES) {
                "amendmentType은 MODIFY 또는 CANCEL이어야 합니다: $amendmentType"
            }
            return OrderAmendment().apply {
                this.order = order
                this.amendmentType = amendmentType
                this.beforeQuantity = beforeQuantity
                this.afterQuantity = afterQuantity
                this.beforeLimitPrice = beforeLimitPrice
                this.afterLimitPrice = afterLimitPrice
                this.reason = reason
            }
        }
    }
}