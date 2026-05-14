package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.application.order.query.OrderListQuery
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.domain.enums.TradingMode
import java.time.Instant
import java.util.Optional

interface OrderRepositoryCustom {
    fun searchOrders(query: OrderListQuery): List<Order>
    fun findByIdAndAccountId(orderId: Long, accountId: Long): Optional<Order>

    /** LOCAL 모드 계좌의 ticker별 미체결(PENDING/PARTIAL) 주문 */
    fun findActiveLocalOrdersByTicker(ticker: String): List<Order>

    /** GTD 만료된 미체결 주문 */
    fun findExpiredOrders(now: Instant): List<Order>

    /** KIS_PAPER 미체결 주문 (externalOrderId 있는 것만) */
    fun findPendingKisPaperOrders(): List<Order>

    /** KIS 체결통보를 로컬 주문과 매칭하기 위한 활성 주문 조회 */
    fun findActiveKisOrderByExternalOrderId(
        externalOrderId: String,
        tradingMode: TradingMode,
        accountScope: String? = null,
    ): Optional<Order>
}
