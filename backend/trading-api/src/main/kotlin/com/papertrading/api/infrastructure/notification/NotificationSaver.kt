package com.papertrading.api.infrastructure.notification

import com.papertrading.api.domain.entity.notification.Notification
import com.papertrading.api.infrastructure.persistence.NotificationRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class NotificationSaver(
    private val repository: NotificationRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun save(notification: Notification): Notification {
        return repository.save(notification)
    }
}
