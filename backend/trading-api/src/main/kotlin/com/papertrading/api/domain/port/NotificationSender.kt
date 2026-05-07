 package com.papertrading.api.domain.port

fun interface NotificationSender {
    fun send(message: String): Boolean
}
