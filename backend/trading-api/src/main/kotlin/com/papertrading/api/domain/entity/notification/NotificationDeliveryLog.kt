package com.papertrading.api.domain.entity.notification

import com.papertrading.api.domain.enums.DeliveryStatus
import com.papertrading.api.domain.enums.NotificationEventType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "notification_delivery_log")
class NotificationDeliveryLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    val eventType: NotificationEventType,

    @Column(name = "source_ref", nullable = false, length = 255)
    val sourceRef: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: DeliveryStatus = DeliveryStatus.PENDING,

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0,

    @Column(name = "last_error", columnDefinition = "text")
    var lastError: String? = null,

    @Column(name = "message", nullable = false, columnDefinition = "text")
    val message: String,

    @Column(name = "delivered_at")
    var deliveredAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.EPOCH,
) {
    @PrePersist
    fun onPrePersist() {
        createdAt = Instant.now()
    }

    fun markDelivering(): NotificationDeliveryLog {
        status = DeliveryStatus.DELIVERING
        return this
    }

    fun markDelivered(): NotificationDeliveryLog {
        status = DeliveryStatus.DELIVERED
        deliveredAt = Instant.now()
        return this
    }

    fun markFailed(error: String, maxRetries: Int): NotificationDeliveryLog {
        attemptCount++
        lastError = error
        status = if (attemptCount < maxRetries) DeliveryStatus.PENDING else DeliveryStatus.FAILED
        return this
    }
}
