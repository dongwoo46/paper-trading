package com.papertrading.api.domain.model

import com.papertrading.api.domain.model.base.BaseAuditEntity
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
@Table(name = "risk_policies")
class RiskPolicy(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    var account: Account? = null,

    @Column(name = "max_position_ratio", precision = 5, scale = 4)
    var maxPositionRatio: BigDecimal? = null,

    @Column(name = "max_daily_loss", precision = 20, scale = 4)
    var maxDailyLoss: BigDecimal? = null,

    @Column(name = "max_order_amount", precision = 20, scale = 4)
    var maxOrderAmount: BigDecimal? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
) : BaseAuditEntity()
