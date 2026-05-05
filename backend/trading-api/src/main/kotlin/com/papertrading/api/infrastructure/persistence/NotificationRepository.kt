package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.notification.Notification
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByAccountIdAndIsReadFalseOrderByCreatedAtDesc(accountId: Long): List<Notification>
}
