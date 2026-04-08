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
import java.time.LocalDate

@Entity
@Table(name = "strategy_performance_snapshots")
class StrategyPerformanceSnapshot(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "strategy_id", nullable = false)
    var strategy: Strategy? = null,

    @Column(name = "period_start", nullable = false)
    var periodStart: LocalDate? = null,

    @Column(name = "period_end", nullable = false)
    var periodEnd: LocalDate? = null,

    @Column(name = "total_return", nullable = false, precision = 10, scale = 4)
    var totalReturn: BigDecimal = BigDecimal.ZERO,

    @Column(name = "sharpe_ratio", precision = 10, scale = 4)
    var sharpeRatio: BigDecimal? = null,

    @Column(name = "max_drawdown", precision = 10, scale = 4)
    var maxDrawdown: BigDecimal? = null,

    @Column(name = "win_rate", precision = 10, scale = 4)
    var winRate: BigDecimal? = null,

    @Column(name = "total_trades", nullable = false)
    var totalTrades: Int = 0
) : BaseTimeEntity()
