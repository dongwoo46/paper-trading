package com.papertrading.api.application.order

import com.papertrading.api.infrastructure.kis.KisExecutionNotice
import com.papertrading.api.infrastructure.persistence.ExecutionRepository
import com.papertrading.api.infrastructure.persistence.OrderRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KisExecutionNoticeService(
    private val orderRepository: OrderRepository,
    private val executionRepository: ExecutionRepository,
    private val executionProcessor: ExecutionProcessor,
) {
    private val log = KotlinLogging.logger {}

    @Transactional
    fun handle(notice: KisExecutionNotice) {
        if (executionRepository.findByExternalExecutionId(notice.externalExecutionId).isPresent) {
            log.info {
                "duplicate KIS execution notice ignored: mode=${notice.mode}, externalOrderId=${notice.externalOrderId}, externalExecutionId=${notice.externalExecutionId}"
            }
            return
        }

        val order = orderRepository.findActiveKisOrderByExternalOrderId(
            externalOrderId = notice.externalOrderId,
            tradingMode = notice.mode,
            accountScope = notice.accountScope,
        )
            .orElse(null)
        val orderId = order?.id
        if (orderId == null) {
            log.warn {
                "KIS execution notice ignored because order was not found: mode=${notice.mode}, externalOrderId=${notice.externalOrderId}, accountScope=${notice.accountScope}"
            }
            return
        }

        executionProcessor.fill(
            orderId = orderId,
            fillPrice = notice.executedPrice,
            fillQty = notice.executedQty,
            externalExecutionId = notice.externalExecutionId,
            executedAt = notice.executedAt,
        )
    }
}
