package com.papertrading.api.domain.entity.portfolio

import com.papertrading.api.domain.entity.account.Account
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
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 일별 잔고 스냅샷
 * 매일 장 마감 후 계좌의 예수금·평가금액·총자산·손익을 집계 저장.
 * 수익률 차트, 기간별 성과 조회의 원천 데이터.
 */
@Entity
@Table(
    name = "daily_balances",
    uniqueConstraints = [UniqueConstraint(name = "uk_daily_balances_account_date", columnNames = ["account_id", "balance_date"])]
)
class DailyBalance protected constructor() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account
        private set

    @Column(name = "balance_date", nullable = false)
    lateinit var balanceDate: LocalDate
        private set

    @Column(name = "deposit", nullable = false, precision = 20, scale = 4)
    lateinit var deposit: BigDecimal
        private set

    @Column(name = "evaluation_amount", nullable = false, precision = 20, scale = 4)
    lateinit var evaluationAmount: BigDecimal
        private set

    @Column(name = "total_asset", nullable = false, precision = 20, scale = 4)
    lateinit var totalAsset: BigDecimal
        private set

    @Column(name = "total_pnl", nullable = false, precision = 20, scale = 4)
    lateinit var totalPnl: BigDecimal
        private set

    @Column(name = "daily_pnl", nullable = false, precision = 20, scale = 4)
    lateinit var dailyPnl: BigDecimal
        private set

    companion object {
        fun create(
            account: Account,
            balanceDate: LocalDate,
            deposit: BigDecimal,
            evaluationAmount: BigDecimal,
            totalAsset: BigDecimal,
            totalPnl: BigDecimal,
            dailyPnl: BigDecimal
        ): DailyBalance = DailyBalance().apply {
            this.account = account
            this.balanceDate = balanceDate
            this.deposit = deposit
            this.evaluationAmount = evaluationAmount
            this.totalAsset = totalAsset
            this.totalPnl = totalPnl
            this.dailyPnl = dailyPnl
        }
    }
}
