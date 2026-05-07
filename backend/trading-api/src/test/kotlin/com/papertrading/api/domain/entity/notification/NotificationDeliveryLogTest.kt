package com.papertrading.api.domain.entity.notification

import com.papertrading.api.domain.enums.DeliveryStatus
import com.papertrading.api.domain.enums.NotificationEventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class NotificationDeliveryLogTest {

    private fun createNotification(attemptCount: Int = 0): Notification {
        val notification = Notification.create(
            eventType = NotificationEventType.EXECUTION_FILLED,
            sourceRef = "execution:42",
            title = NotificationEventType.EXECUTION_FILLED.name,
            message = "체결 알림",
        )
        // attemptCount를 반영하기 위해 필요한 횟수만큼 markFailed 호출
        repeat(attemptCount) { notification.markFailed("init", Int.MAX_VALUE) }
        return notification
    }

    @Test
    fun `markDelivering은 status를 DELIVERING으로 변경한다`() {
        val notification = Notification.create(
            eventType = NotificationEventType.EXECUTION_FILLED,
            sourceRef = "execution:42",
            title = NotificationEventType.EXECUTION_FILLED.name,
            message = "체결 알림",
        )
        assertEquals(DeliveryStatus.PENDING, notification.status)

        notification.markDelivering()

        assertEquals(DeliveryStatus.DELIVERING, notification.status)
    }

    @Test
    fun `markDelivered는 status를 DELIVERED로 변경하고 deliveredAt을 설정한다`() {
        val notification = Notification.create(
            eventType = NotificationEventType.EXECUTION_FILLED,
            sourceRef = "execution:42",
            title = NotificationEventType.EXECUTION_FILLED.name,
            message = "체결 알림",
        )
        assertNull(notification.deliveredAt)

        notification.markDelivered()

        assertEquals(DeliveryStatus.DELIVERED, notification.status)
        assertNotNull(notification.deliveredAt)
    }

    @Test
    fun `markFailed - attemptCount가 maxRetries 미만이면 PENDING 상태 유지 및 attemptCount 증가`() {
        val notification = Notification.create(
            eventType = NotificationEventType.EXECUTION_FILLED,
            sourceRef = "execution:42",
            title = NotificationEventType.EXECUTION_FILLED.name,
            message = "체결 알림",
        )
        val maxRetries = 3

        notification.markFailed("connection timeout", maxRetries)

        assertEquals(DeliveryStatus.PENDING, notification.status)
        assertEquals(1, notification.attemptCount)
        assertEquals("connection timeout", notification.lastError)
    }

    @Test
    fun `markFailed - attemptCount가 maxRetries 이상이면 FAILED 상태로 변경`() {
        val maxRetries = 3
        // attemptCount를 maxRetries - 1로 만들기 위해 2번 markFailed 호출 (maxRetries 초과 없이)
        val notification = Notification.create(
            eventType = NotificationEventType.EXECUTION_FILLED,
            sourceRef = "execution:42",
            title = NotificationEventType.EXECUTION_FILLED.name,
            message = "체결 알림",
        )
        repeat(maxRetries - 1) { notification.markFailed("init", Int.MAX_VALUE) }

        notification.markFailed("final failure", maxRetries)

        assertEquals(DeliveryStatus.FAILED, notification.status)
        assertEquals(maxRetries, notification.attemptCount)
        assertEquals("final failure", notification.lastError)
    }

    @Test
    fun `markFailed - attemptCount가 이미 maxRetries를 초과한 경우에도 FAILED 상태`() {
        val maxRetries = 2
        val notification = Notification.create(
            eventType = NotificationEventType.EXECUTION_FILLED,
            sourceRef = "execution:42",
            title = NotificationEventType.EXECUTION_FILLED.name,
            message = "체결 알림",
        )
        repeat(5) { notification.markFailed("init", Int.MAX_VALUE) }

        notification.markFailed("overflow error", maxRetries)

        assertEquals(DeliveryStatus.FAILED, notification.status)
    }

    @Test
    fun `create - sourceRef가 blank이면 예외가 발생한다`() {
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            Notification.create(
                eventType = NotificationEventType.EXECUTION_FILLED,
                sourceRef = "  ",
                title = "제목",
                message = "메시지",
            )
        }
    }

    @Test
    fun `create - title이 blank이면 예외가 발생한다`() {
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            Notification.create(
                eventType = NotificationEventType.EXECUTION_FILLED,
                sourceRef = "execution:1",
                title = "",
                message = "메시지",
            )
        }
    }

    @Test
    fun `create - message가 blank이면 예외가 발생한다`() {
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            Notification.create(
                eventType = NotificationEventType.EXECUTION_FILLED,
                sourceRef = "execution:1",
                title = "제목",
                message = "  ",
            )
        }
    }
}
