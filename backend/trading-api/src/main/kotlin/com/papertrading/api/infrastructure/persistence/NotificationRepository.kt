package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.notification.Notification
import com.papertrading.api.domain.enums.DeliveryStatus
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository : JpaRepository<Notification, Long>, NotificationRepositoryCustom {
    fun findByAccountIdAndIsReadFalseOrderByCreatedAtDesc(accountId: Long): List<Notification>

    fun findByStatusAndAttemptCountLessThan(
        status: DeliveryStatus,
        maxAttempts: Int,
    ): List<Notification>
}
