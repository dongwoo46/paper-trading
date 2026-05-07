package com.papertrading.api.domain.entity.settlement

import com.papertrading.api.domain.entity.base.BaseTimeEntity
import com.papertrading.api.domain.entity.order.Execution
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 정산-체결 조인 테이블
 * Settlement가 어떤 Execution들을 포함하는지 추적한다.
 * 부분 체결(PARTIAL) 여러 건이 하나의 Settlement로 합산될 때 사용.
 */
@Entity
@Table(
    name = "settlement_executions",
    uniqueConstraints = [UniqueConstraint(name = "uk_settlement_executions", columnNames = ["settlement_id", "execution_id"])]
)
class SettlementExecution protected constructor() : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_id", nullable = false)
    lateinit var settlement: Settlement
        private set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_id", nullable = false)
    lateinit var execution: Execution
        private set

    companion object {
        fun create(
            settlement: Settlement,
            execution: Execution,
        ): SettlementExecution =
            SettlementExecution().apply {
                this.settlement = settlement
                this.execution = execution
            }
    }
}
