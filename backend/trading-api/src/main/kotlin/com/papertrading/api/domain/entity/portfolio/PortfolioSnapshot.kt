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
    name = "portfolio_snapshots",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_portfolio_snapshots_account_date_ticker",
            columnNames = ["account_id", "business_date", "ticker"]
        )
    ]
)
class PortfolioSnapshot protected constructor() : BaseTimeEntity() {

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

    @Column(name = "ticker", nullable = false, length = 20)
    lateinit var ticker: String
        private set

    @Column(name = "quantity", nullable = false, precision = 20, scale = 8)
    lateinit var quantity: BigDecimal
        private set

    @Column(name = "avg_buy_price", nullable = false, precision = 20, scale = 4)
    lateinit var avgBuyPrice: BigDecimal
        private set

    @Column(name = "close_price", nullable = false, precision = 20, scale = 4)
    lateinit var closePrice: BigDecimal
        private set

    @Column(name = "market_value", nullable = false, precision = 20, scale = 4)
    lateinit var marketValue: BigDecimal
        private set

    @Column(name = "weight", nullable = false, precision = 10, scale = 6)
    lateinit var weight: BigDecimal
        private set

    @Column(name = "unrealized_pnl", nullable = false, precision = 20, scale = 4)
    lateinit var unrealizedPnl: BigDecimal
        private set

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
        private set

    fun refresh(
        quantity: BigDecimal,
        avgBuyPrice: BigDecimal,
        closePrice: BigDecimal,
        marketValue: BigDecimal,
        weight: BigDecimal,
        unrealizedPnl: BigDecimal
    ) {
        this.quantity = quantity
        this.avgBuyPrice = avgBuyPrice
        this.closePrice = closePrice
        this.marketValue = marketValue
        this.weight = weight
        this.unrealizedPnl = unrealizedPnl
    }

    companion object {
        fun create(
            account: Account,
            businessDate: LocalDate,
            ticker: String,
            quantity: BigDecimal,
            avgBuyPrice: BigDecimal,
            closePrice: BigDecimal,
            marketValue: BigDecimal,
            weight: BigDecimal,
            unrealizedPnl: BigDecimal
        ): PortfolioSnapshot = PortfolioSnapshot().apply {
            this.account = account
            this.businessDate = businessDate
            this.ticker = ticker
            this.quantity = quantity
            this.avgBuyPrice = avgBuyPrice
            this.closePrice = closePrice
            this.marketValue = marketValue
            this.weight = weight
            this.unrealizedPnl = unrealizedPnl
        }
    }
}
