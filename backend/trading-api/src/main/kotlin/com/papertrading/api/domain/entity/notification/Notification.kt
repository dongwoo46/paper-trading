package com.papertrading.api.domain.entity.notification

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.base.BaseTimeEntity
import com.papertrading.api.domain.enums.DeliveryStatus
import com.papertrading.api.domain.enums.NotificationEventType
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
import java.time.Instant

/**
 * 알림 (Aggregate Root)
 * 인앱 알림 + Slack 전송 이력을 통합한 엔티티.
 * account가 null이면 시스템 이벤트(계좌 귀속 없음)를 의미한다.
 * status: PENDING → DELIVERING → DELIVERED | FAILED (Slack 전송 상태)
 * isRead: 인앱 읽음 여부
 */
@Entity
@Table(name = "notifications")
class Notification protected constructor() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    var account: Account? = null
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    lateinit var eventType: NotificationEventType
        private set

    @Column(name = "source_ref", nullable = false, length = 255)
    lateinit var sourceRef: String
        private set

    @Column(name = "title", nullable = false, length = 200)
    lateinit var title: String
        private set

    @Column(name = "message", nullable = false, columnDefinition = "text")
    lateinit var message: String
        private set

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: DeliveryStatus = DeliveryStatus.PENDING
        private set

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0
        private set

    @Column(name = "last_error", columnDefinition = "text")
    var lastError: String? = null
        private set

    @Column(name = "ref_order_id")
    var refOrderId: Long? = null
        private set

    @Column(name = "ref_execution_id")
    var refExecutionId: Long? = null
        private set

    @Column(name = "delivered_at")
    var deliveredAt: Instant? = null
        private set

    fun markAsRead() {
        isRead = true
    }

    fun markDelivering(): Notification {
        status = DeliveryStatus.DELIVERING
        return this
    }

    fun markDelivered(): Notification {
        status = DeliveryStatus.DELIVERED
        deliveredAt = Instant.now()
        return this
    }

    fun markFailed(error: String, maxRetries: Int): Notification {
        attemptCount++
        lastError = error
        status = if (attemptCount < maxRetries) DeliveryStatus.PENDING else DeliveryStatus.FAILED
        return this
    }

    companion object {
        fun create(
            eventType: NotificationEventType,
            sourceRef: String,
            title: String,
            message: String,
            account: Account? = null,
            refOrderId: Long? = null,
            refExecutionId: Long? = null,
        ): Notification {
            require(sourceRef.isNotBlank()) { "sourceRef는 비어있을 수 없습니다." }
            require(title.isNotBlank()) { "title은 비어있을 수 없습니다." }
            require(message.isNotBlank()) { "message는 비어있을 수 없습니다." }
            return Notification().apply {
                this.eventType = eventType
                this.sourceRef = sourceRef
                this.title = title
                this.message = message
                this.account = account
                this.refOrderId = refOrderId
                this.refExecutionId = refExecutionId
            }
        }
    }
}
