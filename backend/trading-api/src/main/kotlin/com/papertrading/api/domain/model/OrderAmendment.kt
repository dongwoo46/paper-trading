package com.papertrading.api.domain.model

import com.papertrading.api.domain.model.base.BaseTimeEntity
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

@Entity
@Table(name = "order_amendments")
class OrderAmendment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order? = null,

    @Column(name = "amendment_type", nullable = false, length = 20)
    var amendmentType: String? = null,

    @Column(name = "before_quantity", precision = 20, scale = 8)
    var beforeQuantity: BigDecimal? = null,

    @Column(name = "after_quantity", precision = 20, scale = 8)
    var afterQuantity: BigDecimal? = null,

    @Column(name = "before_limit_price", precision = 20, scale = 4)
    var beforeLimitPrice: BigDecimal? = null,

    @Column(name = "after_limit_price", precision = 20, scale = 4)
    var afterLimitPrice: BigDecimal? = null,

    @Column(name = "reason", length = 500)
    var reason: String? = null
) : BaseTimeEntity()
