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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(
    name = "settlements",
    uniqueConstraints = [UniqueConstraint(name = "uk_settlements_order", columnNames = ["order_id"])]
)
class Settlement(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    var account: Account? = null,

    @Column(name = "ticker", nullable = false, length = 20)
    var ticker: String? = null,

    @Column(name = "realized_pnl", nullable = false, precision = 20, scale = 4)
    var realizedPnl: BigDecimal = BigDecimal.ZERO,

    @Column(name = "fee", nullable = false, precision = 20, scale = 4)
    var fee: BigDecimal = BigDecimal.ZERO,

    @Column(name = "tax", nullable = false, precision = 20, scale = 4)
    var tax: BigDecimal = BigDecimal.ZERO,

    @Column(name = "net_pnl", nullable = false, precision = 20, scale = 4)
    var netPnl: BigDecimal = BigDecimal.ZERO,

    @Column(name = "currency", nullable = false, length = 3)
    var currency: String = "KRW",

    @Column(name = "fx_rate_at_settlement", precision = 15, scale = 6)
    var fxRateAtSettlement: BigDecimal? = null,

    @Column(name = "krw_net_pnl", nullable = false, precision = 20, scale = 4)
    var krwNetPnl: BigDecimal = BigDecimal.ZERO,

    @Column(name = "settled_at", nullable = false)
    var settledAt: Instant? = null
) : BaseTimeEntity()
