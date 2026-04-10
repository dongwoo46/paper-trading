package com.papertrading.api.domain.model

import com.papertrading.api.domain.enums.ApprovalStatus
import com.papertrading.api.domain.enums.StrategySourceType
import com.papertrading.api.domain.model.base.BaseAuditEntity
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
class Strategy(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    var account: Account? = null,

    @Column(name = "name", nullable = false, length = 200)
    var name: String? = null,

    @Column(name = "description", columnDefinition = "text")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    var sourceType: StrategySourceType? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    var approvalStatus: ApprovalStatus = ApprovalStatus.DRAFT,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = false,

    @Column(name = "sharpe_ratio", precision = 10, scale = 4)
    var sharpeRatio: BigDecimal? = null,

    @Column(name = "max_drawdown", precision = 10, scale = 4)
    var maxDrawdown: BigDecimal? = null,

    @Column(name = "win_rate", precision = 10, scale = 4)
    var winRate: BigDecimal? = null,

    @Column(name = "avg_return", precision = 10, scale = 4)
    var avgReturn: BigDecimal? = null
) : BaseAuditEntity() {
    fun approve() {
        approvalStatus = ApprovalStatus.APPROVED
    }

    fun reject() {
        approvalStatus = ApprovalStatus.REJECTED
        isActive = false
    }

    fun activate() {
        check(approvalStatus == ApprovalStatus.APPROVED) {
            "APPROVED 상태의 전략만 활성화할 수 있습니다."
        }
        isActive = true
    }
}
