package com.papertrading.api.application.order

import com.papertrading.api.application.order.query.ExecutionQuery
import com.papertrading.api.application.order.query.OrderListQuery
import com.papertrading.api.common.exception.OrderNotFoundException
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.order.Execution
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.enums.OrderType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.infrastructure.persistence.ExecutionRepository
import com.papertrading.api.infrastructure.persistence.OrderRepository
import com.papertrading.api.support.withId
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional

class OrderQueryServiceTest {
    private val orderRepository = mockk<OrderRepository>()
    private val executionRepository = mockk<ExecutionRepository>()
    private val service = OrderQueryService(orderRepository, executionRepository)

    @Test
    fun `getOrder returns mapped result`() {
        val order = sampleOrder()
        every { orderRepository.findByIdAndAccountId(10L, 1L) } returns Optional.of(order)

        val result = service.getOrder(1L, 10L)
        assertEquals(10L, result.orderId)
    }

    @Test
    fun `listOrders uses repository search`() {
        every { orderRepository.searchOrders(any()) } returns listOf(sampleOrder())

        val result = service.listOrders(OrderListQuery(accountId = 1L, ticker = "005930"))
        assertEquals(1, result.size)
    }

    @Test
    fun `listExecutions uses repository search`() {
        every { orderRepository.findByIdAndAccountId(10L, 1L) } returns Optional.of(sampleOrder())
        every { executionRepository.searchExecutions(any()) } returns listOf(sampleExecution())

        val result = service.listExecutions(ExecutionQuery(accountId = 1L, orderId = 10L))
        assertEquals(1, result.size)
    }

    @Test
    fun `getExecution throws when not found`() {
        every { orderRepository.findByIdAndAccountId(10L, 1L) } returns Optional.of(sampleOrder())
        every { executionRepository.findByIdAndOrderIdAndAccountId(99L, 10L, 1L) } returns Optional.empty()

        assertThrows<NoSuchElementException> {
            service.getExecution(1L, 10L, 99L)
        }
    }

    @Test
    fun `listExecutions throws when order ownership mismatch`() {
        every { orderRepository.findByIdAndAccountId(10L, 1L) } returns Optional.empty()

        assertThrows<OrderNotFoundException> {
            service.listExecutions(ExecutionQuery(accountId = 1L, orderId = 10L))
        }
    }

    private fun sampleAccount(): Account = Account.create(
        accountName = "a",
        accountType = AccountType.STOCK,
        tradingMode = TradingMode.LOCAL,
        initialDeposit = BigDecimal("1000000"),
    ).withId(1L)

    private fun sampleOrder(): Order = Order.create(
        account = sampleAccount(),
        ticker = "005930",
        marketType = MarketType.KOSPI,
        orderType = OrderType.LIMIT,
        orderSide = OrderSide.BUY,
        orderCondition = OrderCondition.DAY,
        quantity = BigDecimal("1"),
        limitPrice = BigDecimal("70000"),
        lockedAmount = BigDecimal("70000"),
        idempotencyKey = "idemp-1",
    ).withId(10L).also { setCreatedAt(it, Instant.parse("2026-05-01T00:00:00Z")) }

    private fun sampleExecution(): Execution = Execution.create(
        order = sampleOrder(),
        account = sampleAccount(),
        ticker = "005930",
        executedQuantity = BigDecimal("1"),
        executedPrice = BigDecimal("70000"),
        krwExecutedPrice = BigDecimal("70000"),
        externalExecutionId = "exec-1",
        executedAt = Instant.parse("2026-05-01T00:00:00Z"),
    ).also {
        val field = Execution::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(it, 77L)
    }

    private fun setCreatedAt(order: Order, createdAt: Instant) {
        var type: Class<*>? = order::class.java
        while (type != null) {
            runCatching {
                val f = type.getDeclaredField("createdAt")
                f.isAccessible = true
                f.set(order, createdAt)
                return
            }
            type = type.superclass
        }
        error("createdAt field not found in hierarchy")
    }
}
