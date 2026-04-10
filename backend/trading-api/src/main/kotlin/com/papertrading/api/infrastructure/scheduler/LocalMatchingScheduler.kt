package com.papertrading.api.infrastructure.scheduler

import com.papertrading.api.application.order.OrderCommandService
import com.papertrading.api.application.order.command.CancelOrderCommand
import com.papertrading.api.infrastructure.persistence.OrderRepository
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * LOCAL 모드 보조 스케줄러 (30초 주기)
 * - GTD 만료 주문 자동 CANCELLED 처리
 * - trading-api 재시작 후 누락 tick 복구 (유효 시세 있는 PENDING 주문 재매칭)
 */
@Component
class LocalMatchingScheduler(
    private val orderRepository: OrderRepository,
    private val orderCommandService: OrderCommandService,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(fixedDelay = 30_000)
    fun cancelExpiredGtdOrders() {
        val expired = orderRepository.findExpiredOrders(Instant.now())
        if (expired.isEmpty()) return

        log.info { "GTD 만료 주문 취소 처리: ${expired.size}건" }
        for (order in expired) {
            val accountId = order.account?.id ?: continue
            val orderId = order.id ?: continue
            runCatching {
                orderCommandService.cancelOrder(accountId, orderId, CancelOrderCommand("GTD 만료"))
            }.onFailure {
                log.warn { "GTD 취소 실패: orderId=$orderId, reason=${it.message}" }
            }
        }
    }
}
