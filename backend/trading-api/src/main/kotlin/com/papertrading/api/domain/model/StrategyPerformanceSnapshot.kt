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

/**
 * 전략 기간별 성과 스냅샷
 * 특정 기간(periodStart~periodEnd)의 수익률·샤프지수·MDD·승률을 집계 보관.
 * research-service 백테스트 또는 실 운용 결과를 주기적으로 저장.
 */
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
