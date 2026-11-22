package com.papertrading.api.application.order

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.papertrading.api.application.order.port.BrokerExecutionNotice
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
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional

class BrokerExecutionNoticeServiceTest {

    private val orderRepository = mockk<OrderRepository>()
    private val executionRepository = mockk<ExecutionRepository>()
    private val executionProcessor = mockk<ExecutionProcessor>()
    private val service = BrokerExecutionNoticeService(orderRepository, executionRepository, executionProcessor)

    @Test
    fun `duplicate notice does not mutate financial state again`() {
        val notice = sampleNotice()
        every { executionRepository.findByExternalExecutionId(notice.externalExecutionId) } returns savedExecution()

        service.handle(notice)

        verify(exactly = 0) { orderRepository.findActiveKisOrderByExternalOrderId(any(), any(), any()) }
        verify(exactly = 0) { executionProcessor.fillExternal(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `new notice resolves order by external id, mode and account scope then fills with scoped execution id`() {
        val notice = sampleNotice()
        val order = sampleOrder()
        every { executionRepository.findByExternalExecutionId(notice.externalExecutionId) } returns Optional.empty()
        every {
            orderRepository.findActiveKisOrderByExternalOrderId("10000001", TradingMode.KIS_PAPER, "12345678-01")
        } returns Optional.of(order)
        every {
            executionProcessor.fillExternal(10L, BigDecimal("70000"), BigDecimal("3"), EXECUTION_ID, notice.executedAt)
        } just runs

        service.handle(notice)

        verify(exactly = 1) {
            orderRepository.findActiveKisOrderByExternalOrderId("10000001", TradingMode.KIS_PAPER, "12345678-01")
        }
        verify(exactly = 1) {
            executionProcessor.fillExternal(10L, BigDecimal("70000"), BigDecimal("3"), EXECUTION_ID, notice.executedAt)
        }
    }

    @Test
    fun `duplicate detection is scoped by external execution id account scope`() {
        val notice = sampleNotice()
        val order = sampleOrder()
        every { executionRepository.findByExternalExecutionId(EXECUTION_ID) } returns Optional.empty()
        every {
            orderRepository.findActiveKisOrderByExternalOrderId("10000001", TradingMode.KIS_PAPER, "12345678-01")
        } returns Optional.of(order)
        every {
            executionProcessor.fillExternal(10L, BigDecimal("70000"), BigDecimal("3"), EXECUTION_ID, notice.executedAt)
        } just runs

        service.handle(notice)

        verify(exactly = 1) {
            executionRepository.findByExternalExecutionId(EXECUTION_ID)
        }
        verify(exactly = 1) {
            executionProcessor.fillExternal(10L, BigDecimal("70000"), BigDecimal("3"), EXECUTION_ID, notice.executedAt)
        }
    }

    private fun savedExecution() = Optional.of(mockk<Execution>(relaxed = true))

    private fun logsOf(block: () -> Unit): List<String> {
        val logger = LoggerFactory.getLogger(BrokerExecutionNoticeService::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().also { it.start() }
        logger.addAppender(appender)
        try {
            block()
        } finally {
            logger.detachAppender(appender)
        }
        return appender.list.map { it.formattedMessage }
    }

    @Test
    fun `duplicate notice log masks the account number of the KIS execution id`() {
        val notice = sampleNotice()
        every { executionRepository.findByExternalExecutionId(notice.externalExecutionId) } returns savedExecution()

        val logs = logsOf { service.handle(notice) }

        assertTrue(logs.isNotEmpty(), "duplicate notice must be logged")
        assertTrue(logs.none { "12345678" in it }, "account number must be masked: $logs")
        assertTrue(logs.any { "10000001:00003" in it }, "order and execution numbers stay: $logs")
    }

    @Test
    fun `unmatched notice log masks the account number in execution id and account scope`() {
        val notice = sampleNotice()
        every { executionRepository.findByExternalExecutionId(notice.externalExecutionId) } returns Optional.empty()
        every { orderRepository.findActiveKisOrderByExternalOrderId(any(), any(), any()) } returns Optional.empty()

        val logs = logsOf { service.handle(notice) }

        assertTrue(logs.isNotEmpty(), "unmatched notice must be logged")
        assertTrue(logs.none { "12345678" in it }, "account number must be masked: $logs")
        assertTrue(logs.any { "10000001" in it }, "external order id stays: $logs")
    }

    private fun sampleNotice() = BrokerExecutionNotice(
        mode = TradingMode.KIS_PAPER,
        externalOrderId = "10000001",
        externalExecutionId = EXECUTION_ID,
        executedQty = BigDecimal("3"),
        executedPrice = BigDecimal("70000"),
        executedAt = Instant.parse("2026-05-01T00:30:15Z"),
        accountScope = "12345678-01",
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

    private companion object {
        const val EXECUTION_ID = "KIS_PAPER:12345678-01:10000001:00003"
    }
}
