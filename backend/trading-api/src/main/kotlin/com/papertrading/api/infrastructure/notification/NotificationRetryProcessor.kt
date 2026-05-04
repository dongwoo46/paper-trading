package com.papertrading.api.infrastructure.notification

import com.papertrading.api.domain.entity.NotificationDeliveryLog
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class NotificationRetryProcessor(
    private val repository: NotificationDeliveryLogRepository,
) {
    /**
     * Atomically claims the record from PENDING → DELIVERING.
     * Returns the updated log if claimed, or null if another instance already claimed it.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markDelivering(log: NotificationDeliveryLog): NotificationDeliveryLog? {
        val claimed = repository.claimForDelivery(log.id!!)
        if (claimed == 0) return null
        log.markDelivering()
        return log
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markDelivered(log: NotificationDeliveryLog) {
        log.markDelivered()
        repository.save(log)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markFailed(log: NotificationDeliveryLog, error: String, maxRetries: Int) {
        log.markFailed(error, maxRetries)
        repository.save(log)
    }
}
