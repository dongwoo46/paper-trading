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
import java.time.Instant

/**
 * 리스크 이벤트 이력
 * RiskPolicy 한도 위반(최대 손실 초과, 단건 주문 한도 초과 등) 발생 시 기록.
 * 알림 발송 및 사후 감사 추적 용도. 삭제·수정 없이 append-only로 유지.
 */
@Entity
@Table(name = "risk_events")
class RiskEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    var account: Account? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_policy_id")
    var riskPolicy: RiskPolicy? = null,

    @Column(name = "event_type", nullable = false, length = 50)
    var eventType: String? = null,

    @Column(name = "description", length = 500)
    var description: String? = null,

    @Column(name = "triggered_at", nullable = false)
    var triggeredAt: Instant = Instant.now()
) : BaseTimeEntity()
