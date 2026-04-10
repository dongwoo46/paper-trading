package com.papertrading.api.domain.model

import com.papertrading.api.domain.model.base.BaseTimeEntity
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
class SettlementExecution(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_id", nullable = false)
    var settlement: Settlement? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_id", nullable = false)
    var execution: Execution? = null
) : BaseTimeEntity()
