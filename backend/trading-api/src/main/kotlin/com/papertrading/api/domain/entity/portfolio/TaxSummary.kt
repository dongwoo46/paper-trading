package com.papertrading.api.domain.entity.portfolio

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.base.BaseAuditEntity
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

/**
 * 연간 세금 정산 요약
 * 연도별 실현손익·과세대상손익·예상세금을 집계 보관.
 * 연말 세금 신고 지원 및 세후 수익률 계산 용도.
 */
@Entity
@Table(
    name = "tax_summaries",
    uniqueConstraints = [UniqueConstraint(name = "uk_tax_summaries_account_year", columnNames = ["account_id", "tax_year"])]
)
class TaxSummary protected constructor() : BaseAuditEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account
        private set

    @Column(name = "tax_year", nullable = false)
    var taxYear: Int = 0
        private set

    @Column(name = "total_realized_pnl", nullable = false, precision = 20, scale = 4)
    lateinit var totalRealizedPnl: BigDecimal
        private set

    @Column(name = "taxable_pnl", nullable = false, precision = 20, scale = 4)
    lateinit var taxablePnl: BigDecimal
        private set

    @Column(name = "estimated_tax", nullable = false, precision = 20, scale = 4)
    lateinit var estimatedTax: BigDecimal
        private set

    fun recalculate(totalRealizedPnl: BigDecimal, taxablePnl: BigDecimal, estimatedTax: BigDecimal) {
        require(estimatedTax >= BigDecimal.ZERO) { "예상 세금은 0 이상이어야 합니다." }
        this.totalRealizedPnl = totalRealizedPnl
        this.taxablePnl = taxablePnl
        this.estimatedTax = estimatedTax
    }

    companion object {
        fun create(
            account: Account,
            taxYear: Int,
            totalRealizedPnl: BigDecimal,
            taxablePnl: BigDecimal,
            estimatedTax: BigDecimal
        ): TaxSummary = TaxSummary().apply {
            this.account = account
            this.taxYear = taxYear
            this.totalRealizedPnl = totalRealizedPnl
            this.taxablePnl = taxablePnl
            this.estimatedTax = estimatedTax
        }
    }
}
