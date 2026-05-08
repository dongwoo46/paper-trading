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
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

/**
 * 전략 버전 이력
 * 전략 규칙(rules JSONB) 변경 시마다 버전 행 추가 — 기존 버전 보존.
 * backtestRunId: research-service의 백테스트 실행 ID(외부 참조, UUID).
 */
@Entity
@Table(
    name = "strategy_versions",
    uniqueConstraints = [UniqueConstraint(name = "uk_strategy_versions_strategy_version", columnNames = ["strategy_id", "version_no"])]
)
class StrategyVersion protected constructor() : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "strategy_id", nullable = false)
    lateinit var strategy: Strategy
        private set

    @Column(name = "version_no", nullable = false)
    var versionNo: Int = 0
        private set

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rules", nullable = false, columnDefinition = "jsonb")
    lateinit var rules: String
        private set

    @Column(name = "backtest_run_id")
    var backtestRunId: UUID? = null
        private set

    @Column(name = "change_note", columnDefinition = "text")
    var changeNote: String? = null
        private set

    @Column(name = "created_by", length = 100)
    var createdBy: String? = null
        private set

    fun attachBacktestRun(backtestRunId: UUID) {
        this.backtestRunId = backtestRunId
    }

    fun updateChangeNote(changeNote: String?) {
        this.changeNote = changeNote?.trim()?.ifBlank { null }
    }

    companion object {
        fun create(
            strategy: Strategy,
            versionNo: Int,
            rules: String,
            createdBy: String? = null,
            changeNote: String? = null,
            backtestRunId: UUID? = null
        ): StrategyVersion {
            require(versionNo > 0) { "versionNo는 1 이상이어야 합니다." }
            require(rules.isNotBlank()) { "rules는 비어 있을 수 없습니다." }
            val normalizedCreatedBy = createdBy?.trim()?.ifBlank { null }
            require(normalizedCreatedBy == null || normalizedCreatedBy.length <= 100) {
                "createdBy 길이는 100자를 초과할 수 없습니다."
            }
            return StrategyVersion().apply {
                this.strategy = strategy
                this.versionNo = versionNo
                this.rules = rules
                this.createdBy = normalizedCreatedBy
                this.changeNote = changeNote?.trim()?.ifBlank { null }
                this.backtestRunId = backtestRunId
            }
        }
    }
}
