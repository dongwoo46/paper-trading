package com.papertrading.api.infrastructure.persistence

interface NotificationRepositoryCustom {
    fun claimForDelivery(id: Long): Int
}