package com.papertrading.api.characterization.kis

import com.papertrading.api.application.notification.SlackNotificationRequestedEvent
import com.papertrading.api.domain.event.ExecutionFilledEvent
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.event.EventListener
import java.util.concurrent.CopyOnWriteArrayList

// KIS 경계 특성화 시험(step 8)이 «밖으로 나가는 알림·이벤트» 를 읽는 자리. Slack 전송기 자체는 건드리지 않고 발행된 이벤트만 모은다.
class KisBoundaryEventCapture {
    val slackEvents = CopyOnWriteArrayList<SlackNotificationRequestedEvent>()
    val executionFilledEvents = CopyOnWriteArrayList<ExecutionFilledEvent>()

    @EventListener
    fun onSlack(event: SlackNotificationRequestedEvent) {
        slackEvents.add(event)
    }

    @EventListener
    fun onExecutionFilled(event: ExecutionFilledEvent) {
        executionFilledEvents.add(event)
    }

    fun slackEventsOf(accountId: Long): List<SlackNotificationRequestedEvent> {
        val ofAccount = slackEvents.filter { it.accountId == accountId }
        return ofAccount
    }
}

@TestConfiguration
class KisBoundaryEventCaptureConfig {
    @Bean
    fun kisBoundaryEventCapture() = KisBoundaryEventCapture()
}
