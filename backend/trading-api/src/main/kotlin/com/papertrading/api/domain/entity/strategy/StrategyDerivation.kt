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

/**
 * 전략 파생 계보
 * 기존 전략(parent)을 기반으로 새 전략(child)을 파생시킬 때 관계를 기록.
 * derivationType: FORK(독립 파생) | TUNE(파라미터 조정) | ENSEMBLE(앙상블 결합)
 */
@Entity
@Table(name = "strategy_derivations")
class StrategyDerivation protected constructor() : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_strategy_id", nullable = false)
    lateinit var parentStrategy: Strategy
        private set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_strategy_id", nullable = false)
    lateinit var childStrategy: Strategy
        private set

    @Column(name = "derivation_type", nullable = false, length = 30)
    lateinit var derivationType: String
        private set

    @Column(name = "note", columnDefinition = "text")
    var note: String? = null
        private set

    fun updateNote(newNote: String?) {
        note = newNote?.trim()?.ifBlank { null }
    }

    companion object {
        private val ALLOWED_DERIVATION_TYPES = setOf("FORK", "TUNE", "ENSEMBLE")

        fun create(
            parentStrategy: Strategy,
            childStrategy: Strategy,
            derivationType: String,
            note: String? = null
        ): StrategyDerivation {
            require(parentStrategy !== childStrategy) { "부모와 자식 전략은 같을 수 없습니다." }
            val normalizedType = derivationType.trim().uppercase()
            require(normalizedType in ALLOWED_DERIVATION_TYPES) {
                "derivationType은 FORK/TUNE/ENSEMBLE 중 하나여야 합니다."
            }

            return StrategyDerivation().apply {
                this.parentStrategy = parentStrategy
                this.childStrategy = childStrategy
                this.derivationType = normalizedType
                this.note = note?.trim()?.ifBlank { null }
            }
        }
    }
}
