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
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 일별 포트폴리오 구성 스냅샷
 * 매일 장 마감 시점의 종목별 보유 비중·평가금액을 JSONB로 저장.
 * composition 예시: [{"ticker":"005930","weight":0.35,"evaluation":3500000}, ...]
 */
@Entity
@Table(
    name = "portfolio_snapshots",
    uniqueConstraints = [UniqueConstraint(name = "uk_portfolio_snapshots_account_date", columnNames = ["account_id", "snapshot_date"])]
)
class PortfolioSnapshot protected constructor() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account
        private set

    @Column(name = "snapshot_date", nullable = false)
    lateinit var snapshotDate: LocalDate
        private set

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "composition", columnDefinition = "jsonb")
    var composition: String? = null
        private set

    @Column(name = "total_evaluation", nullable = false, precision = 20, scale = 4)
    lateinit var totalEvaluation: BigDecimal
        private set

    companion object {
        fun create(
            account: Account,
            snapshotDate: LocalDate,
            totalEvaluation: BigDecimal,
            composition: String? = null
        ): PortfolioSnapshot = PortfolioSnapshot().apply {
            this.account = account
            this.snapshotDate = snapshotDate
            this.totalEvaluation = totalEvaluation
            this.composition = composition
        }
    }
}
