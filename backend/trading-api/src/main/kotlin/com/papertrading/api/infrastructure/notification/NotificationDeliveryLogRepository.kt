package com.papertrading.api.infrastructure.notification

import com.papertrading.api.domain.entity.NotificationDeliveryLog
import com.papertrading.api.domain.enums.DeliveryStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface NotificationDeliveryLogRepository : JpaRepository<NotificationDeliveryLog, Long> {
    fun findByStatusAndAttemptCountLessThan(
        status: DeliveryStatus,
        maxAttempts: Int,
    ): List<NotificationDeliveryLog>

    @Modifying
    @Query("UPDATE NotificationDeliveryLog n SET n.status = 'DELIVERING' WHERE n.id = :id AND n.status = 'PENDING'")
    fun claimForDelivery(@Param("id") id: Long): Int
}
