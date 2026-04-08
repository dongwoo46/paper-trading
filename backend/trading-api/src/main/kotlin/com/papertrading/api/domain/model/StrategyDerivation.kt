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
