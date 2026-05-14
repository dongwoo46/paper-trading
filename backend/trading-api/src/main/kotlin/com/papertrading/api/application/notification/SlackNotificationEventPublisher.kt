package com.papertrading.api.application.notification

import com.papertrading.api.domain.enums.NotificationEventType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant

// 알림 이벤트를 발생, 주문 이벤트 , 주문 취소 이벤트 ,체결성공, 체결 실패 이벤트
@Component
class SlackNotificationEventPublisher(
    private val eventPublisher: ApplicationEventPublisher,
) {
    // 주문 생성 알림 이벤트 발행
    fun publishOrderCreated(accountId: Long, orderId: Long, message: String, occurredAt: Instant = Instant.now()) {
        eventPublisher.publishEvent(
            SlackNotificationRequestedEvent(
                eventType = NotificationEventType.ORDER_CREATED,
                accountId = accountId,
                orderId = orderId,
                executionId = null,
                riskEventId = null,
                message = message,
                occurredAt = occurredAt,
            )
        )
    }

    // 주문 취소 알림 이벤트 발행
    fun publishOrderCanceled(accountId: Long, orderId: Long, message: String, occurredAt: Instant = Instant.now()) {
        eventPublisher.publishEvent(
            SlackNotificationRequestedEvent(
                eventType = NotificationEventType.ORDER_CANCELED,
                accountId = accountId,
                orderId = orderId,
                executionId = null,
                riskEventId = null,
                message = message,
                occurredAt = occurredAt,
            )
        )
    }

    // 주문 체결 실패 알림 이벤트 발행
    fun publishExecutionFailed(accountId: Long, orderId: Long, executionId: Long, message: String, occurredAt: Instant = Instant.now()) {
        eventPublisher.publishEvent(
            SlackNotificationRequestedEvent(
                eventType = NotificationEventType.EXECUTION_FAILED,
                accountId = accountId,
                orderId = orderId,
                executionId = executionId,
                riskEventId = null,
                message = message,
                occurredAt = occurredAt,
            )
        )
    }

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
