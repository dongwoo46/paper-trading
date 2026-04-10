package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.model.Order
import java.time.Instant

interface OrderRepositoryCustom {
    /** LOCAL 모드 계좌의 ticker별 미체결(PENDING/PARTIAL) 주문 */
    fun findActiveLocalOrdersByTicker(ticker: String): List<Order>

    /** GTD 만료된 미체결 주문 */
    fun findExpiredOrders(now: Instant): List<Order>

    /** KIS_PAPER 미체결 주문 (externalOrderId 있는 것만) */
    fun findPendingKisPaperOrders(): List<Order>
}
