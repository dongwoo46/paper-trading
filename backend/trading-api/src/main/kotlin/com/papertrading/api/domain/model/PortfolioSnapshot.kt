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
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(
    name = "portfolio_snapshots",
    uniqueConstraints = [UniqueConstraint(name = "uk_portfolio_snapshots_account_date", columnNames = ["account_id", "snapshot_date"])]
)
class PortfolioSnapshot(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    var account: Account? = null,

    @Column(name = "snapshot_date", nullable = false)
    var snapshotDate: LocalDate? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "composition", columnDefinition = "jsonb")
    var composition: String? = null,

    @Column(name = "total_evaluation", nullable = false, precision = 20, scale = 4)
    var totalEvaluation: BigDecimal = BigDecimal.ZERO
) : BaseTimeEntity()
