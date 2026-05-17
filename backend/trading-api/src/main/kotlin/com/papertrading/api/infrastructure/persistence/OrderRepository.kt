package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.domain.enums.OrderSide
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.util.Optional

interface OrderRepository : JpaRepository<Order, Long>, OrderRepositoryCustom {
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    fun findByIdWithOptimisticLock(@Param("id") id: Long): Optional<Order>

    fun findByAccountIdAndOrderStatusIn(accountId: Long, statuses: List<OrderStatus>): List<Order>
    fun existsByAccountIdAndIdempotencyKey(accountId: Long, idempotencyKey: String): Boolean
    fun findByAccountIdAndIdempotencyKey(accountId: Long, idempotencyKey: String): Order?
    fun findByAccountIdAndOrderGroupId(accountId: Long, orderGroupId: String): Order?
    fun findByAccountIdOrderByCreatedAtDesc(accountId: Long): List<Order>

    @Query(
        """
        SELECT COALESCE(SUM(o.quantity - o.filledQuantity), 0)
        FROM Order o
        WHERE o.account.id = :accountId
          AND o.ticker = :ticker
          AND o.orderSide = com.papertrading.api.domain.enums.OrderSide.SELL
          AND o.orderStatus IN :statuses
        """
    )
    fun sumOpenSellQuantity(
        @Param("accountId") accountId: Long,
        @Param("ticker") ticker: String,
        @Param("statuses") statuses: Collection<OrderStatus> = listOf(OrderStatus.PENDING, OrderStatus.PARTIAL),
    ): BigDecimal

    @Query(
        """
        SELECT COUNT(o) > 0
        FROM Order o
        WHERE o.account.id = :accountId
          AND o.ticker = :ticker
          AND o.orderSide = :orderSide
          AND o.orderStatus IN :statuses
          AND o.orderGroupId IS NULL
        """
    )
    fun existsOpenOrderByAccountTickerSideWithoutGroup(
        @Param("accountId") accountId: Long,
        @Param("ticker") ticker: String,
        @Param("orderSide") orderSide: OrderSide = OrderSide.SELL,
        @Param("statuses") statuses: Collection<OrderStatus> = listOf(OrderStatus.PENDING, OrderStatus.PARTIAL),
    ): Boolean
}
