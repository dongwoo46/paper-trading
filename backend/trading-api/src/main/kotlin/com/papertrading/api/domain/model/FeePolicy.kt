package com.papertrading.api.domain.model

import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.model.base.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "fee_policies")
class FeePolicy(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "trading_mode", nullable = false, length = 20)
    var tradingMode: TradingMode? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", nullable = false, length = 20)
    var marketType: MarketType? = null,

    @Column(name = "fee_rate", nullable = false, precision = 10, scale = 6)
    var feeRate: BigDecimal = BigDecimal.ZERO,

    @Column(name = "min_fee", nullable = false, precision = 20, scale = 4)
    var minFee: BigDecimal = BigDecimal.ZERO,

    @Column(name = "effective_from", nullable = false)
    var effectiveFrom: Instant? = null,

    @Column(name = "effective_until")
    var effectiveUntil: Instant? = null
) : BaseTimeEntity()
