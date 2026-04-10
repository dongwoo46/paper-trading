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
class TaxSummary(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    var account: Account? = null,

    @Column(name = "tax_year", nullable = false)
    var taxYear: Int = 0,

    @Column(name = "total_realized_pnl", nullable = false, precision = 20, scale = 4)
    var totalRealizedPnl: BigDecimal = BigDecimal.ZERO,

    @Column(name = "taxable_pnl", nullable = false, precision = 20, scale = 4)
    var taxablePnl: BigDecimal = BigDecimal.ZERO,

    @Column(name = "estimated_tax", nullable = false, precision = 20, scale = 4)
    var estimatedTax: BigDecimal = BigDecimal.ZERO
) : BaseAuditEntity()
