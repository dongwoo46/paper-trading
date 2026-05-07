package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.notification.Notification
import com.papertrading.api.domain.enums.DeliveryStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByAccountIdAndIsReadFalseOrderByCreatedAtDesc(accountId: Long): List<Notification>

    fun findByStatusAndAttemptCountLessThan(
        status: DeliveryStatus,
        maxAttempts: Int,
    ): List<Notification>

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'DELIVERING' WHERE n.id = :id AND n.status = 'PENDING'")
    fun claimForDelivery(@Param("id") id: Long): Int
}