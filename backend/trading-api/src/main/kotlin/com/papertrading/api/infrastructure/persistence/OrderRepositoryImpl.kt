package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.model.Order
import com.papertrading.api.domain.model.QOrder
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.Instant

class OrderRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : OrderRepositoryCustom {

    private val o = QOrder.order

    override fun findActiveLocalOrdersByTicker(ticker: String): List<Order> =
        queryFactory
            .select(o)
            .from(o)
            .where(
                o.ticker.eq(ticker),
                o.orderStatus.`in`(OrderStatus.PENDING, OrderStatus.PARTIAL),
                o.account.tradingMode.eq(TradingMode.LOCAL),
            )
            .fetch()

    override fun findExpiredOrders(now: Instant): List<Order> =
        queryFactory
            .select(o)
            .from(o)
            .where(
                o.orderStatus.`in`(OrderStatus.PENDING, OrderStatus.PARTIAL),
                o.expireAt.isNotNull,
                o.expireAt.before(now),
            )
            .fetch()

    override fun findPendingKisPaperOrders(): List<Order> =
        queryFactory
            .select(o)
            .from(o)
            .join(o.account).fetchJoin()
            .where(
                o.account.tradingMode.eq(TradingMode.KIS_PAPER),
                o.orderStatus.`in`(OrderStatus.PENDING, OrderStatus.PARTIAL),
                o.externalOrderId.isNotNull,
            )
            .fetch()
}
