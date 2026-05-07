package com.papertrading.api.infrastructure.notification

import com.papertrading.api.domain.entity.notification.Notification
import com.papertrading.api.domain.enums.DeliveryStatus
import com.papertrading.api.infrastructure.persistence.NotificationRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class NotificationPersistenceTxService(
    private val repository: NotificationRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun save(notification: Notification): Notification =
        repository.save(notification)

    /**
     * PENDING → DELIVERING 원자적 전환.
     * 다른 인스턴스가 이미 선점했으면 null 반환.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markDelivering(notification: Notification): Notification? {
        val claimed = repository.claimForDelivery(notification.id ?: return null)
        if (claimed == 0) return null
        notification.markDelivering()
        return notification
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markDelivered(notification: Notification) {
        notification.markDelivered()
        repository.save(notification)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markFailed(notification: Notification, error: String, maxRetries: Int) {
        notification.markFailed(error, maxRetries)
        repository.save(notification)
    }

    fun findPendingForRetry(maxAttempts: Int): List<Notification> =
        repository.findByStatusAndAttemptCountLessThan(DeliveryStatus.PENDING, maxAttempts)
}
