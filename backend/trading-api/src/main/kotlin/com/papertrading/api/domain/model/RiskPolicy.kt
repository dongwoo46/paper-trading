package com.papertrading.api.domain.model

import com.papertrading.api.domain.model.base.BaseAuditEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal

/**
 * 리스크 정책 (Account Aggregate 내부 구성 요소)
 * 계좌별 주문 한도를 설정한다. 주문 접수 시 이 정책을 검사해 초과 주문을 차단.
 * 생성은 반드시 Account.createRiskPolicy()를 통해서만 수행.
 * isActive=true인 정책이 현재 적용 중. upsert 시 기존 정책 비활성화 후 신규 생성(이력 보존).
 * - maxPositionRatio: 단일 종목 최대 보유 비중 (0~1, 예: 0.2 = 계좌 총액의 20%)
 * - maxDailyLoss: 일일 최대 허용 손실액
 * - maxOrderAmount: 단건 주문 최대 금액
 */
@Entity
@Table(name = "risk_policies")
class RiskPolicy(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    var account: Account? = null,

    @Column(name = "max_position_ratio", precision = 5, scale = 4)
    var maxPositionRatio: BigDecimal? = null,

    @Column(name = "max_daily_loss", precision = 20, scale = 4)
    var maxDailyLoss: BigDecimal? = null,

    @Column(name = "max_order_amount", precision = 20, scale = 4)
    var maxOrderAmount: BigDecimal? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
) : BaseAuditEntity()
