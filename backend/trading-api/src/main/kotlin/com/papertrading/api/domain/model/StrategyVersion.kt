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
import java.util.UUID

@Entity
@Table(
    name = "strategy_versions",
    uniqueConstraints = [UniqueConstraint(name = "uk_strategy_versions_strategy_version", columnNames = ["strategy_id", "version_no"])]
)
class StrategyVersion(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "strategy_id", nullable = false)
    var strategy: Strategy? = null,

    @Column(name = "version_no", nullable = false)
    var versionNo: Int = 0,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rules", nullable = false, columnDefinition = "jsonb")
    var rules: String? = null,

    @Column(name = "backtest_run_id")
    var backtestRunId: UUID? = null,

    @Column(name = "change_note", columnDefinition = "text")
    var changeNote: String? = null,

    @Column(name = "created_by", length = 100)
    var createdBy: String? = null
) : BaseTimeEntity()
