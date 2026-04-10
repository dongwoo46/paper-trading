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

/**
 * 전략 파생 계보
 * 기존 전략(parent)을 기반으로 새 전략(child)을 파생시킬 때 관계를 기록.
 * derivationType: FORK(독립 파생) | TUNE(파라미터 조정) | ENSEMBLE(앙상블 결합)
 */
@Entity
@Table(name = "strategy_derivations")
class StrategyDerivation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_strategy_id", nullable = false)
    var parentStrategy: Strategy? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_strategy_id", nullable = false)
    var childStrategy: Strategy? = null,

    @Column(name = "derivation_type", nullable = false, length = 30)
    var derivationType: String? = null,

    @Column(name = "note", columnDefinition = "text")
    var note: String? = null
) : BaseTimeEntity()
