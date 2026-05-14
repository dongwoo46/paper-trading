package com.papertrading.api.application.order

import com.papertrading.api.support.withId
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.enums.OrderType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.order.Execution
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.infrastructure.kis.KisExecutionNotice
import com.papertrading.api.infrastructure.persistence.ExecutionRepository
import com.papertrading.api.infrastructure.persistence.OrderRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional

class KisExecutionNoticeServiceTest {

    private val orderRepository = mockk<OrderRepository>()
    private val executionRepository = mockk<ExecutionRepository>()
    private val executionProcessor = mockk<ExecutionProcessor>()
    private val service = KisExecutionNoticeService(orderRepository, executionRepository, executionProcessor)

    @Test
    fun `duplicate notice does not mutate financial state again`() {
        val notice = sampleNotice()
        every { executionRepository.findByExternalExecutionId(notice.externalExecutionId) } returns Optional.of(mockk<Execution>(relaxed = true))

        service.handle(notice)

        verify(exactly = 0) { orderRepository.findActiveKisOrderByExternalOrderId(any(), any(), any()) }
        verify(exactly = 0) { executionProcessor.fillExternal(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `new notice resolves by external order id mode and account scope then fills with scoped KIS external execution id`() {
        val notice = sampleNotice()
        val order = sampleOrder()
        every { executionRepository.findByExternalExecutionId(notice.externalExecutionId) } returns Optional.empty()
        every {
            orderRepository.findActiveKisOrderByExternalOrderId("10000001", TradingMode.KIS_PAPER, "12345678-01")
        } returns Optional.of(order)
        every {
            executionProcessor.fillExternal(10L, BigDecimal("70000"), BigDecimal("3"), "KIS_PAPER:12345678-01:10000001:00003", notice.executedAt)
        } just runs

        service.handle(notice)

        verify(exactly = 1) {
            orderRepository.findActiveKisOrderByExternalOrderId("10000001", TradingMode.KIS_PAPER, "12345678-01")
        }
        verify(exactly = 1) {
            executionProcessor.fillExternal(10L, BigDecimal("70000"), BigDecimal("3"), "KIS_PAPER:12345678-01:10000001:00003", notice.executedAt)
        }
    }

    @Test
    fun `duplicate detection is scoped by external execution id account scope`() {
        val notice = sampleNotice()
        val order = sampleOrder()
        every { executionRepository.findByExternalExecutionId("KIS_PAPER:12345678-01:10000001:00003") } returns Optional.empty()
        every {
            orderRepository.findActiveKisOrderByExternalOrderId("10000001", TradingMode.KIS_PAPER, "12345678-01")
        } returns Optional.of(order)
        every {
            executionProcessor.fillExternal(10L, BigDecimal("70000"), BigDecimal("3"), "KIS_PAPER:12345678-01:10000001:00003", notice.executedAt)
        } just runs

        service.handle(notice)

        verify(exactly = 1) {
            executionRepository.findByExternalExecutionId("KIS_PAPER:12345678-01:10000001:00003")
        }
        verify(exactly = 1) {
            executionProcessor.fillExternal(10L, BigDecimal("70000"), BigDecimal("3"), "KIS_PAPER:12345678-01:10000001:00003", notice.executedAt)
        }
    }

    private fun sampleNotice() = KisExecutionNotice(
        mode = TradingMode.KIS_PAPER,
        channelId = "H0STCNI9",
        externalOrderId = "10000001",
        externalExecutionId = "KIS_PAPER:12345678-01:10000001:00003",
        ticker = "005930",
        side = OrderSide.BUY,
        executedQty = BigDecimal("3"),
        executedPrice = BigDecimal("70000"),
        executedAt = Instant.parse("2026-05-01T00:30:15Z"),
        accountNumber = "12345678",
        accountProductCode = "01",
    )

    private fun sampleOrder(): Order {
        val account = Account.create(
            accountName = "kis",
            accountType = AccountType.STOCK,
            tradingMode = TradingMode.KIS_PAPER,
            initialDeposit = BigDecimal("1000000"),
            externalAccountId = "12345678-01",
        ).withId(1L)
        return Order.create(
            account = account,
            ticker = "005930",
            marketType = MarketType.KOSPI,
            orderType = OrderType.LIMIT,
            orderSide = OrderSide.BUY,
            orderCondition = OrderCondition.DAY,
            quantity = BigDecimal("3"),
            limitPrice = BigDecimal("70000"),
            lockedAmount = BigDecimal("210000"),
            idempotencyKey = "key",
        ).withId(10L).also { it.assignExternalOrderId("10000001") }
    }
}
