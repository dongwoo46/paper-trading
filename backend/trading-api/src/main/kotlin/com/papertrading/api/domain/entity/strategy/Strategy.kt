package com.papertrading.api.domain.entity.strategy

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.base.BaseAuditEntity
import com.papertrading.api.domain.enums.ApprovalStatus
import com.papertrading.api.domain.enums.StrategySourceType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

/**
 * 전략 (Aggregate Root)
 * 매매 전략 메타 정보 및 승인 상태 관리.
 * sourceType: HUMAN(수동) | AI_QUANT | AI_RECOMMENDED | HYBRID
 * approvalStatus: DRAFT → PENDING_REVIEW → APPROVED/REJECTED
 * AI 생성 전략은 APPROVED 이후에만 실거래 활성화 가능(activate() 내부 guard).
 * sharpeRatio·maxDrawdown·winRate: research-service 백테스트 결과를 수신해 저장.
 */
@Entity
@Table(name = "strategies")
class Strategy protected constructor() : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account
        private set

    @Column(name = "name", nullable = false, length = 200)
    lateinit var name: String
        private set

    @Column(name = "description", columnDefinition = "text")
    var description: String? = null
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    lateinit var sourceType: StrategySourceType
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    var approvalStatus: ApprovalStatus = ApprovalStatus.DRAFT
        private set

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = false
        private set

    @Column(name = "is_cancelled", nullable = false)
    var isCancelled: Boolean = false
        private set

    @Column(name = "cancelled_at")
    var cancelledAt: Instant? = null
        private set

    @Column(name = "cancel_reason", length = 500)
    var cancelReason: String? = null
        private set

    @Column(name = "last_executed_at")
    var lastExecutedAt: Instant? = null
        private set

    @Column(name = "execution_count", nullable = false)
    var executionCount: Long = 0
        private set

    @Column(name = "sharpe_ratio", precision = 10, scale = 4)
    var sharpeRatio: BigDecimal? = null
        private set

    @Column(name = "max_drawdown", precision = 10, scale = 4)
    var maxDrawdown: BigDecimal? = null
        private set

    @Column(name = "win_rate", precision = 10, scale = 4)
    var winRate: BigDecimal? = null
        private set

    @Column(name = "avg_return", precision = 10, scale = 4)
    var avgReturn: BigDecimal? = null
        private set

    fun rename(newName: String) {
        val normalized = newName.trim()
        require(normalized.isNotBlank()) { "전략명은 비어 있을 수 없습니다." }
        require(normalized.length <= 200) { "전략명은 200자를 초과할 수 없습니다." }
        name = normalized
    }

    fun updateDescription(newDescription: String?) {
        description = newDescription?.trim()?.ifBlank { null }
    }

    fun requestReview() {
        check(!isCancelled) { "취소된 전략은 검토 요청할 수 없습니다." }
        check(approvalStatus == ApprovalStatus.DRAFT || approvalStatus == ApprovalStatus.REJECTED) {
            "DRAFT 또는 REJECTED 상태에서만 검토 요청할 수 있습니다."
        }
        check(!isActive) { "활성화된 전략은 검토 요청할 수 없습니다." }
        approvalStatus = ApprovalStatus.PENDING_REVIEW
    }

    fun approve() {
        check(!isCancelled) { "취소된 전략은 승인할 수 없습니다." }
        check(approvalStatus == ApprovalStatus.PENDING_REVIEW) {
            "PENDING_REVIEW 상태의 전략만 승인할 수 있습니다."
        }
        approvalStatus = ApprovalStatus.APPROVED
    }

    fun reject() {
        check(!isCancelled) { "취소된 전략은 반려할 수 없습니다." }
        check(approvalStatus == ApprovalStatus.PENDING_REVIEW) {
            "PENDING_REVIEW 상태의 전략만 반려할 수 있습니다."
        }
        approvalStatus = ApprovalStatus.REJECTED
        isActive = false
    }

    fun activate() {
        check(!isCancelled) { "취소된 전략은 활성화할 수 없습니다." }
        check(approvalStatus == ApprovalStatus.APPROVED) {
            "APPROVED 상태의 전략만 활성화할 수 있습니다."
        }
        check(!isActive) { "이미 활성화된 전략입니다." }
        isActive = true
    }

    fun deactivate() {
        check(isActive) { "이미 비활성화된 전략입니다." }
        isActive = false
    }

    fun cancel(reason: String, cancelledAt: Instant = Instant.now()) {
        check(!isCancelled) { "이미 취소된 전략입니다." }
        val normalizedReason = reason.trim()
        require(normalizedReason.isNotBlank()) { "취소 사유는 비어 있을 수 없습니다." }
        require(normalizedReason.length <= 500) { "취소 사유는 500자를 초과할 수 없습니다." }
        require(!cancelledAt.isAfter(Instant.now().plusSeconds(5))) {
            "cancelledAt은 미래 시각일 수 없습니다."
        }
        isCancelled = true
        this.cancelledAt = cancelledAt
        this.cancelReason = normalizedReason
        isActive = false
    }

    fun markExecuted(executedAt: Instant = Instant.now()) {
        check(!isCancelled) { "취소된 전략은 실행 처리할 수 없습니다." }
        check(isActive) { "비활성 전략은 실행 처리할 수 없습니다." }
        require(!executedAt.isAfter(Instant.now().plusSeconds(5))) {
            "executedAt은 미래 시각일 수 없습니다."
        }
        lastExecutedAt = executedAt
        executionCount += 1
    }

    fun hasExecuted(): Boolean = executionCount > 0

    fun updatePerformance(
        sharpeRatio: BigDecimal?,
        maxDrawdown: BigDecimal?,
        winRate: BigDecimal?,
        avgReturn: BigDecimal?
    ) {
        validateSharpeRatio(sharpeRatio)
        validateMaxDrawdown(maxDrawdown)
        validateWinRate(winRate)
        validateAvgReturn(avgReturn)
        this.sharpeRatio = sharpeRatio
        this.maxDrawdown = maxDrawdown
        this.winRate = winRate
        this.avgReturn = avgReturn
    }

    companion object {
        fun create(
            account: Account,
            name: String,
            sourceType: StrategySourceType,
            description: String? = null
        ): Strategy {
            val normalized = name.trim()
            require(normalized.isNotBlank()) { "전략명은 비어 있을 수 없습니다." }
            require(normalized.length <= 200) { "전략명은 200자를 초과할 수 없습니다." }
            return Strategy().apply {
                this.account = account
                this.name = normalized
                this.sourceType = sourceType
                this.description = description?.trim()?.ifBlank { null }
                this.approvalStatus = ApprovalStatus.DRAFT
                this.isActive = false
                this.isCancelled = false
                this.cancelledAt = null
                this.cancelReason = null
                this.lastExecutedAt = null
                this.executionCount = 0
            }
        }

        private fun validateSharpeRatio(value: BigDecimal?) {
            require(value == null || value >= BigDecimal("-100.0000")) {
                "sharpeRatio 값이 유효 범위를 벗어났습니다."
            }
        }

        private fun validateMaxDrawdown(value: BigDecimal?) {
            require(value == null || value in BigDecimal("-1.0000")..BigDecimal.ZERO) {
                "maxDrawdown은 -1.0000~0 사이여야 합니다."
            }
        }

        private fun validateWinRate(value: BigDecimal?) {
            require(value == null || value in BigDecimal.ZERO..BigDecimal.ONE) {
                "winRate는 0~1 사이여야 합니다."
            }
        }

        private fun validateAvgReturn(value: BigDecimal?) {
            require(value == null || value >= BigDecimal("-1.0000")) {
                "avgReturn은 -1.0000 이상이어야 합니다."
            }
        }
    }
}
