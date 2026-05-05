package com.papertrading.api.infrastructure.notification

import com.papertrading.api.domain.entity.notification.NotificationDeliveryLog
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class NotificationDeliveryLogSaver(
    private val repository: NotificationDeliveryLogRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun save(log: NotificationDeliveryLog): NotificationDeliveryLog {
        return repository.save(log)
    }
}
