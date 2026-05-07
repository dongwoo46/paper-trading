package com.papertrading.api.domain.entity.notification

import com.papertrading.api.domain.enums.DeliveryStatus
import com.papertrading.api.domain.enums.NotificationEventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NotificationTest {

    private fun makeNotification() = Notification.create(
        eventType = NotificationEventType.EXECUTION_FILLED,
        sourceRef = "execution:1",
        title = "체결 알림",
        message = "주문이 체결되었습니다.",
    )

    // --- create ---

    @Test
    fun `create_정상_생성되면_초기_상태가_올바르게_설정된다`() {
        val notification = makeNotification()

        assertEquals(NotificationEventType.EXECUTION_FILLED, notification.eventType)
        assertEquals("execution:1", notification.sourceRef)
        assertEquals("체결 알림", notification.title)
        assertEquals("주문이 체결되었습니다.", notification.message)
        assertEquals(DeliveryStatus.PENDING, notification.status)
        assertEquals(0, notification.attemptCount)
        assertEquals(false, notification.isRead)
        assertNull(notification.lastError)
        assertNull(notification.deliveredAt)
    }

    @Test
    fun `create_sourceRef가_공백이면_예외를_던진다`() {
        assertThrows<IllegalArgumentException> {
            Notification.create(
                eventType = NotificationEventType.EXECUTION_FILLED,
                sourceRef = "  ",
                title = "제목",
                message = "메시지",
            )
        }
    }

    @Test
    fun `create_title이_공백이면_예외를_던진다`() {
        assertThrows<IllegalArgumentException> {
            Notification.create(
                eventType = NotificationEventType.EXECUTION_FILLED,
                sourceRef = "execution:1",
                title = "",
                message = "메시지",
            )
        }
    }

    @Test
    fun `create_message가_공백이면_예외를_던진다`() {
        assertThrows<IllegalArgumentException> {
            Notification.create(
                eventType = NotificationEventType.EXECUTION_FILLED,
                sourceRef = "execution:1",
                title = "제목",
                message = "  ",
            )
        }
    }

    // --- markAsRead ---

    @Test
    fun `markAsRead_호출하면_isRead가_true로_변경된다`() {
        val notification = makeNotification()

        notification.markAsRead()

        assertTrue(notification.isRead)
    }

    // --- markDelivering ---

    @Test
    fun `markDelivering_호출하면_status가_DELIVERING으로_변경된다`() {
        val notification = makeNotification()

        notification.markDelivering()

        assertEquals(DeliveryStatus.DELIVERING, notification.status)
    }

    // --- markDelivered ---

    @Test
    fun `markDelivered_호출하면_status가_DELIVERED로_변경되고_deliveredAt이_설정된다`() {
        val notification = makeNotification()

        notification.markDelivered()

        assertEquals(DeliveryStatus.DELIVERED, notification.status)
        assertNotNull(notification.deliveredAt)
    }

    // --- markFailed ---

    @Test
    fun `markFailed_attemptCount가_maxRetries_미만이면_PENDING을_유지한다`() {
        val notification = makeNotification()

        notification.markFailed("webhook 오류", maxRetries = 3)

        assertEquals(1, notification.attemptCount)
        assertEquals(DeliveryStatus.PENDING, notification.status)
        assertEquals("webhook 오류", notification.lastError)
    }

    @Test
    fun `markFailed_attemptCount가_maxRetries에_도달하면_FAILED로_변경된다`() {
        val notification = makeNotification()

        notification.markFailed("오류", maxRetries = 2)
        notification.markFailed("오류", maxRetries = 2)

        assertEquals(2, notification.attemptCount)
        assertEquals(DeliveryStatus.FAILED, notification.status)
    }

    @Test
    fun `markFailed_호출할수록_attemptCount가_누적된다`() {
        val notification = makeNotification()

        repeat(3) { notification.markFailed("오류", maxRetries = 5) }

        assertEquals(3, notification.attemptCount)
        assertEquals(DeliveryStatus.PENDING, notification.status)
    }

    @Test
    fun `markFailed_lastError는_마지막_오류_메시지로_덮어쓴다`() {
        val notification = makeNotification()

        notification.markFailed("첫 번째 오류", maxRetries = 3)
        notification.markFailed("두 번째 오류", maxRetries = 3)

        assertEquals("두 번째 오류", notification.lastError)
    }
}