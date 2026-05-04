package com.papertrading.api.domain.entity

import com.papertrading.api.domain.enums.DeliveryStatus
import com.papertrading.api.domain.enums.NotificationEventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class NotificationDeliveryLogTest {

    private fun createLog(attemptCount: Int = 0): NotificationDeliveryLog =
        NotificationDeliveryLog(
            eventType = NotificationEventType.EXECUTION_FILLED,
            sourceRef = "execution:42",
            message = "체결 알림",
            attemptCount = attemptCount,
        )

    @Test
    fun `markDelivering은 status를 DELIVERING으로 변경한다`() {
        val log = createLog()
        assertEquals(DeliveryStatus.PENDING, log.status)

        log.markDelivering()

        assertEquals(DeliveryStatus.DELIVERING, log.status)
    }

    @Test
    fun `markDelivered는 status를 DELIVERED로 변경하고 deliveredAt을 설정한다`() {
        val log = createLog()
        assertNull(log.deliveredAt)

        log.markDelivered()

        assertEquals(DeliveryStatus.DELIVERED, log.status)
        assertNotNull(log.deliveredAt)
    }

    @Test
    fun `markFailed - attemptCount가 maxRetries 미만이면 PENDING 상태 유지 및 attemptCount 증가`() {
        val log = createLog(attemptCount = 0)
        val maxRetries = 3

        log.markFailed("connection timeout", maxRetries)

        assertEquals(DeliveryStatus.PENDING, log.status)
        assertEquals(1, log.attemptCount)
        assertEquals("connection timeout", log.lastError)
    }

    @Test
    fun `markFailed - attemptCount가 maxRetries 이상이면 FAILED 상태로 변경`() {
        val maxRetries = 3
        val log = createLog(attemptCount = maxRetries - 1) // attemptCount == 2, after ++ == 3 == maxRetries

        log.markFailed("final failure", maxRetries)

        assertEquals(DeliveryStatus.FAILED, log.status)
        assertEquals(maxRetries, log.attemptCount)
        assertEquals("final failure", log.lastError)
    }

    @Test
    fun `markFailed - attemptCount가 이미 maxRetries를 초과한 경우에도 FAILED 상태`() {
        val maxRetries = 2
        val log = createLog(attemptCount = 5)

        log.markFailed("overflow error", maxRetries)

        assertEquals(DeliveryStatus.FAILED, log.status)
    }
}
