package com.papertrading.api.application.order

import com.papertrading.api.infrastructure.kis.KisExecutionNotice
import com.papertrading.api.infrastructure.persistence.ExecutionRepository
import com.papertrading.api.infrastructure.persistence.OrderRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 외부 증권사에서 체결 이벤트가 들어왔을 때, 내부 주문 상태와 체결 내역을 업데이트한다
@Service
class KisExecutionNoticeService(
    private val orderRepository: OrderRepository,
    private val executionRepository: ExecutionRepository,
    private val executionProcessor: ExecutionProcessor,
) {
    private val log = KotlinLogging.logger {}

    @Transactional
    fun handle(notice: KisExecutionNotice) {
        // 이미 체결 통지 처리한 문제인지 확인
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
        ).orElse(null)

        if (order == null) {
            // 내 계좌에서 발생한 체결 통지인데 내부 주문과 매칭되지 않으면 정합성 이상으로 기록한다.
            log.error {
                "KIS execution notice could not be matched to local order: " +
                        "mode=${notice.mode}, externalOrderId=${notice.externalOrderId}, " +
                        "externalExecutionId=${notice.externalExecutionId}, accountScope=${notice.accountScope}, " +
                        "executedPrice=${notice.executedPrice}, executedQty=${notice.executedQty}, executedAt=${notice.executedAt}"
            }
            return
        }

        executionProcessor.fillExternal(
            orderId = order.id!!,
            fillPrice = notice.executedPrice,
            fillQty = notice.executedQty,
            externalExecutionId = notice.externalExecutionId,
            executedAt = notice.executedAt,
        )
    }
}
