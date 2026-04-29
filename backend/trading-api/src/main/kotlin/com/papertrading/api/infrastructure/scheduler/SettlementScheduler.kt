package com.papertrading.api.infrastructure.scheduler

import com.papertrading.api.application.settlement.SettlementCommandService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId

/**
 * 정산 스케줄러
 * 매 영업일 KST 16:30 (장 마감 후) T+2 도래 정산을 일괄 처리한다.
 */
@Component
class SettlementScheduler(
    private val settlementCommandService: SettlementCommandService,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = "0 30 16 * * MON-FRI", zone = "Asia/Seoul")
    fun runDailySettlement() {
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
        log.info { "정산 스케줄러 시작: targetDate=$today" }
        val count = settlementCommandService.processSettlements(today)
        log.info { "정산 스케줄러 완료: 처리 건수=$count" }
    }
}
