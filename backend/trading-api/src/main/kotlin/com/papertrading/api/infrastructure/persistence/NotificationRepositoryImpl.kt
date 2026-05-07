package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.notification.QNotification
import com.papertrading.api.domain.enums.DeliveryStatus
import com.querydsl.jpa.impl.JPAQueryFactory

class NotificationRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : NotificationRepositoryCustom {

    private val notification = QNotification.notification

    override fun claimForDelivery(id: Long): Int =
        queryFactory
            .update(notification)
            .set(notification.status, DeliveryStatus.DELIVERING)
            .where(
                notification.id.eq(id),
                notification.status.eq(DeliveryStatus.PENDING),
            )
            .execute()
            .toInt()
}