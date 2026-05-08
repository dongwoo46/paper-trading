package com.papertrading.api.domain.entity.strategy

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
import java.time.LocalDate

/**
 * 전략 기간별 성과 스냅샷
 * 특정 기간(periodStart~periodEnd)의 수익률·샤프지수·MDD·승률을 집계 보관.
 * research-service 백테스트 또는 실 운용 결과를 주기적으로 저장.
 */
@Entity
@Table(name = "strategy_performance_snapshots")
class StrategyPerformanceSnapshot protected constructor() : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "strategy_id", nullable = false)
    lateinit var strategy: Strategy
        private set

    @Column(name = "period_start", nullable = false)
    lateinit var periodStart: LocalDate
        private set

    @Column(name = "period_end", nullable = false)
    lateinit var periodEnd: LocalDate
        private set

    @Column(name = "total_return", nullable = false, precision = 10, scale = 4)
    var totalReturn: BigDecimal = BigDecimal.ZERO
        private set

    @Column(name = "sharpe_ratio", precision = 10, scale = 4)
    var sharpeRatio: BigDecimal? = null
        private set

    @Column(name = "max_drawdown", precision = 10, scale = 4)
    var maxDrawdown: BigDecimal? = null
        private set

    @Column(name = "win_rate", precision = 10, scale = 4)
    var winRate: BigDecimal? = null
        private set

    @Column(name = "total_trades", nullable = false)
    var totalTrades: Int = 0
        private set

    fun reviseMetrics(
        totalReturn: BigDecimal,
        sharpeRatio: BigDecimal?,
        maxDrawdown: BigDecimal?,
        winRate: BigDecimal?,
        totalTrades: Int
    ) {
        validateMetrics(periodStart, periodEnd, totalReturn, maxDrawdown, winRate, totalTrades)
        this.totalReturn = totalReturn
        this.sharpeRatio = sharpeRatio
        this.maxDrawdown = maxDrawdown
        this.winRate = winRate
        this.totalTrades = totalTrades
    }

    companion object {
        fun create(
            strategy: Strategy,
            periodStart: LocalDate,
            periodEnd: LocalDate,
            totalReturn: BigDecimal,
            sharpeRatio: BigDecimal?,
            maxDrawdown: BigDecimal?,
            winRate: BigDecimal?,
            totalTrades: Int
        ): StrategyPerformanceSnapshot {
            validateMetrics(periodStart, periodEnd, totalReturn, maxDrawdown, winRate, totalTrades)
            return StrategyPerformanceSnapshot().apply {
                this.strategy = strategy
                this.periodStart = periodStart
                this.periodEnd = periodEnd
                this.totalReturn = totalReturn
                this.sharpeRatio = sharpeRatio
                this.maxDrawdown = maxDrawdown
                this.winRate = winRate
                this.totalTrades = totalTrades
            }
        }

        private fun validateMetrics(
            periodStart: LocalDate,
            periodEnd: LocalDate,
            totalReturn: BigDecimal,
            maxDrawdown: BigDecimal?,
            winRate: BigDecimal?,
            totalTrades: Int
        ) {
            require(!periodEnd.isBefore(periodStart)) { "periodEnd는 periodStart보다 빠를 수 없습니다." }
            require(totalReturn >= BigDecimal("-1.0000")) { "totalReturn은 -1.0000 이상이어야 합니다." }
            require(maxDrawdown == null || maxDrawdown in BigDecimal("-1.0000")..BigDecimal.ZERO) {
                "maxDrawdown은 -1.0000~0 사이여야 합니다."
            }
            require(winRate == null || winRate in BigDecimal.ZERO..BigDecimal.ONE) {
                "winRate는 0~1 사이여야 합니다."
            }
            require(totalTrades >= 0) { "totalTrades는 0 이상이어야 합니다." }
        }
    }
}
