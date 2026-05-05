package com.papertrading.api.domain.entity.account

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
import java.time.Instant

/**
 * 리스크 이벤트 이력
 * RiskPolicy 한도 위반(최대 손실 초과, 단건 주문 한도 초과 등) 발생 시 기록.
 * 알림 발송 및 사후 감사 추적 용도. 삭제·수정 없이 append-only로 유지.
 */
@Entity
@Table(name = "risk_events")
class RiskEvent protected constructor() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account
        private set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_policy_id")
    var riskPolicy: RiskPolicy? = null
        private set

    @Column(name = "event_type", nullable = false, length = 50)
    lateinit var eventType: String
        private set

    @Column(name = "description", length = 500)
    var description: String? = null
        private set

    @Column(name = "triggered_at", nullable = false)
    lateinit var triggeredAt: Instant
        private set

    companion object {
        internal fun create(
            account: Account,
            eventType: String,
            riskPolicy: RiskPolicy? = null,
            description: String? = null
        ): RiskEvent = RiskEvent().apply {
            this.account = account
            this.eventType = eventType
            this.riskPolicy = riskPolicy
            this.description = description
            this.triggeredAt = Instant.now()
        }
    }
}
