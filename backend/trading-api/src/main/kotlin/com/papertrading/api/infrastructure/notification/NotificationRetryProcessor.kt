package com.papertrading.api.infrastructure.notification

import com.papertrading.api.domain.entity.notification.Notification
import com.papertrading.api.infrastructure.persistence.NotificationRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class NotificationRetryProcessor(
    private val repository: NotificationRepository,
) {
    /**
     * Atomically claims the record from PENDING → DELIVERING.
     * Returns the updated notification if claimed, or null if another instance already claimed it.
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
}