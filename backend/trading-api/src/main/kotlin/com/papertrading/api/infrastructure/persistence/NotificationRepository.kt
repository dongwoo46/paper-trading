package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.model.Notification
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByAccountIdAndIsReadFalseOrderByCreatedAtDesc(accountId: Long): List<Notification>
}
