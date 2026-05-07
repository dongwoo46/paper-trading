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
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(
    name = "daily_balances",
    uniqueConstraints = [UniqueConstraint(name = "uk_daily_balances_account_date", columnNames = ["account_id", "business_date"])]
)
class DailyBalance protected constructor() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account
        private set

    @Column(name = "business_date", nullable = false)
    lateinit var businessDate: LocalDate
        private set

    @Column(name = "cash_balance", nullable = false, precision = 20, scale = 4)
    lateinit var cashBalance: BigDecimal
        private set

    @Column(name = "stock_market_value", nullable = false, precision = 20, scale = 4)
    lateinit var stockMarketValue: BigDecimal
        private set

    @Column(name = "total_asset_value", nullable = false, precision = 20, scale = 4)
    lateinit var totalAssetValue: BigDecimal
        private set

    @Column(name = "pnl_amount", nullable = false, precision = 20, scale = 4)
    lateinit var pnlAmount: BigDecimal
        private set

    @Column(name = "pnl_rate", nullable = false, precision = 10, scale = 6)
    lateinit var pnlRate: BigDecimal
        private set

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
        private set

    fun refresh(
        cashBalance: BigDecimal,
        stockMarketValue: BigDecimal,
        totalAssetValue: BigDecimal,
        pnlAmount: BigDecimal,
        pnlRate: BigDecimal
    ) {
        this.cashBalance = cashBalance
        this.stockMarketValue = stockMarketValue
        this.totalAssetValue = totalAssetValue
        this.pnlAmount = pnlAmount
        this.pnlRate = pnlRate
    }

    companion object {
        fun create(
            account: Account,
            businessDate: LocalDate,
            cashBalance: BigDecimal,
            stockMarketValue: BigDecimal,
            totalAssetValue: BigDecimal,
            pnlAmount: BigDecimal,
            pnlRate: BigDecimal
        ): DailyBalance = DailyBalance().apply {
            this.account = account
            this.businessDate = businessDate
            this.cashBalance = cashBalance
            this.stockMarketValue = stockMarketValue
            this.totalAssetValue = totalAssetValue
            this.pnlAmount = pnlAmount
            this.pnlRate = pnlRate
        }
    }
}