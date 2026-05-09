package com.papertrading.api.application.notification

import com.papertrading.api.domain.enums.NotificationEventType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant

// 알림 이벤트를 발생
@Component
class SlackNotificationEventPublisher(
    private val eventPublisher: ApplicationEventPublisher,
) {
    // 주문 체결 완료 알림 이벤트 발행
    fun publishExecutionFilled(accountId: Long, orderId: Long, executionId: Long, message: String, occurredAt: Instant) {
        eventPublisher.publishEvent(
            SlackNotificationRequestedEvent(
                eventType = NotificationEventType.EXECUTION_FILLED,
                accountId = accountId,
                orderId = orderId,
                executionId = executionId,
                riskEventId = null,
                message = message,
                occurredAt = occurredAt,
            )
        )
    }

    // 주문 실패 이벤트 발행
    fun publishOrderError(accountId: Long, orderId: Long?, message: String, occurredAt: Instant = Instant.now()) {
        eventPublisher.publishEvent(
            SlackNotificationRequestedEvent(
                eventType = NotificationEventType.ORDER_ERROR,
                accountId = accountId,
                orderId = orderId,
                executionId = null,
                riskEventId = null,
                message = message,
                occurredAt = occurredAt,
            )
        )
    }

    // 리스크 경고 이벤트 발행
    fun publishRiskAlert(accountId: Long, riskEventId: Long?, message: String, occurredAt: Instant = Instant.now()) {
        eventPublisher.publishEvent(
            SlackNotificationRequestedEvent(
                eventType = NotificationEventType.RISK_ALERT,
                accountId = accountId,
                orderId = null,
                executionId = null,
                riskEventId = riskEventId,
                message = message,
                occurredAt = occurredAt,
            )
        )
    }
}

