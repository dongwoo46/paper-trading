package com.papertrading.api.application.position

import com.papertrading.api.application.notification.SlackNotificationEventPublisher
import com.papertrading.api.application.order.AutoExitTriggerAuditInput
import com.papertrading.api.application.order.OrderCommandService
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.entity.position.PositionExitTrigger
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.PriceBasisPolicy
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.enums.TriggerSkipReason
import com.papertrading.api.domain.enums.TriggerState
import com.papertrading.api.domain.enums.TriggerType
import com.papertrading.api.infrastructure.persistence.OrderRepository
import com.papertrading.api.infrastructure.persistence.PositionExitTriggerRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import com.papertrading.api.support.withId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.PessimisticLockingFailureException
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional

class PositionExitTriggerOrchestratorTest {
    private val positionRepository = mockk<PositionRepository>()
    private val triggerRepository = mockk<PositionExitTriggerRepository>()
    private val evaluator = PositionExitTriggerEvaluator()
    private val orderCommandService = mockk<OrderCommandService>()
    private val orderRepository = mockk<OrderRepository>()
    private val notificationEventPublisher = mockk<SlackNotificationEventPublisher>(relaxed = true)
    private val orchestrator = PositionExitTriggerOrchestrator(
        positionRepository,
        triggerRepository,
        evaluator,
        orderCommandService,
        orderRepository,
        notificationEventPublisher,
    )
    private val quoteAt = Instant.parse("2026-05-08T12:00:00Z")

    @Test
    fun `same position and trigger type creates one grouped order with summed exit ratio`() {
        val position = position(quantity = BigDecimal("10.0000"))
        val first = fixedTrigger(100L, TriggerType.STOP_LOSS, BigDecimal("95.0000"), BigDecimal("30.0000"))
        val second = fixedTrigger(101L, TriggerType.STOP_LOSS, BigDecimal("96.0000"), BigDecimal("40.0000"))
        val quantity = slot<BigDecimal>()
        val orderGroupId = slot<String>()
        val audits = slot<List<AutoExitTriggerAuditInput>>()
        stubSuccessfulOrder(position, listOf(first, second), quantity, orderGroupId, audits)

        orchestrator.onQuote("005930", BigDecimal("94.0000"), quoteAt)

        assertEquals(0, BigDecimal("7.00000000").compareTo(quantity.captured))
        assertTrue(orderGroupId.captured.startsWith("auto-exit:10:STOP_LOSS:${quoteAt.toEpochMilli()}:"))
        assertEquals(listOf(AutoExitTriggerAuditInput(100L, 0L), AutoExitTriggerAuditInput(101L, 0L)), audits.captured)
        assertEquals(TriggerState.TRIGGERED, first.state)
        assertEquals(TriggerState.TRIGGERED, second.state)
        verify(exactly = 1) {
            orderCommandService.createGroupedAutoExitSellOrder(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `trigger type creates separate grouped orders`() {
        val position = position(quantity = BigDecimal("10.0000"))
        val stopLoss = fixedTrigger(100L, TriggerType.STOP_LOSS, BigDecimal("100.0000"), BigDecimal("25.0000"))
        val takeProfit = fixedTrigger(101L, TriggerType.TAKE_PROFIT, BigDecimal("100.0000"), BigDecimal("25.0000"))
        every { triggerRepository.findByTickerAndState("005930", TriggerState.ARMED) } returns listOf(stopLoss, takeProfit)
        every { positionRepository.findByIdWithLock(10L) } returns Optional.of(position)
        every { triggerRepository.findArmedGroupForUpdate("005930", 10L, TriggerType.STOP_LOSS) } returns listOf(stopLoss)
        every { triggerRepository.findArmedGroupForUpdate("005930", 10L, TriggerType.TAKE_PROFIT) } returns listOf(takeProfit)
        every { orderRepository.existsOpenOrderByAccountTickerSideWithoutGroup(1L, "005930") } returns false
        every { orderRepository.sumOpenSellQuantity(1L, "005930") } returns BigDecimal.ZERO
        every { orderCommandService.createGroupedAutoExitSellOrder(any(), any(), any(), any(), any(), any()) } returns mockk<Order>()
        every { triggerRepository.saveAll(any<List<PositionExitTrigger>>()) } answers { firstArg() }

        orchestrator.onQuote("005930", BigDecimal("100.0000"), quoteAt)

        assertEquals(TriggerState.TRIGGERED, stopLoss.state)
        assertEquals(TriggerState.TRIGGERED, takeProfit.state)
        verify(exactly = 2) {
            orderCommandService.createGroupedAutoExitSellOrder(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `order quantity caps summed exit ratio and subtracts pending sell quantity`() {
        val position = position(quantity = BigDecimal("10.0000"))
        val first = fixedTrigger(100L, TriggerType.STOP_LOSS, BigDecimal("95.0000"), BigDecimal("80.0000"))
        val second = fixedTrigger(101L, TriggerType.STOP_LOSS, BigDecimal("96.0000"), BigDecimal("40.0000"))
        val quantity = slot<BigDecimal>()
        every { triggerRepository.findByTickerAndState("005930", TriggerState.ARMED) } returns listOf(first, second)
        every { positionRepository.findByIdWithLock(10L) } returns Optional.of(position)
        every { triggerRepository.findArmedGroupForUpdate("005930", 10L, TriggerType.STOP_LOSS) } returns listOf(first, second)
        every { orderRepository.existsOpenOrderByAccountTickerSideWithoutGroup(1L, "005930") } returns false
        every { orderRepository.sumOpenSellQuantity(1L, "005930") } returns BigDecimal("3.0000")
        every {
            orderCommandService.createGroupedAutoExitSellOrder(
                accountId = 1L,
                ticker = "005930",
                marketType = MarketType.KOSPI,
                quantity = capture(quantity),
                orderGroupId = any(),
                triggerAuditInputs = any(),
            )
        } returns mockk<Order>()
        every { triggerRepository.saveAll(any<List<PositionExitTrigger>>()) } answers { firstArg() }

        orchestrator.onQuote("005930", BigDecimal("94.0000"), quoteAt)

        assertEquals(0, BigDecimal("7.00000000").compareTo(quantity.captured))
    }

    @Test
    fun `no available quantity skips fired triggers without creating an order`() {
        val position = position(quantity = BigDecimal("10.0000"))
        val trigger = fixedTrigger(100L, TriggerType.STOP_LOSS, BigDecimal("95.0000"), BigDecimal("100.0000"))
        every { triggerRepository.findByTickerAndState("005930", TriggerState.ARMED) } returns listOf(trigger)
        every { positionRepository.findByIdWithLock(10L) } returns Optional.of(position)
        every { triggerRepository.findArmedGroupForUpdate("005930", 10L, TriggerType.STOP_LOSS) } returns listOf(trigger)
        every { orderRepository.existsOpenOrderByAccountTickerSideWithoutGroup(1L, "005930") } returns false
        every { orderRepository.sumOpenSellQuantity(1L, "005930") } returns BigDecimal("10.0000")
        every { triggerRepository.saveAll(any<List<PositionExitTrigger>>()) } answers { firstArg() }

        orchestrator.onQuote("005930", BigDecimal("94.0000"), quoteAt)

        assertEquals(TriggerState.SKIPPED, trigger.state)
        assertEquals(TriggerSkipReason.SELL_ALREADY_LOCKED, trigger.skipReason)
        verify(exactly = 0) {
            orderCommandService.createGroupedAutoExitSellOrder(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `manual sell conflict skips fired triggers`() {
        val position = position(quantity = BigDecimal("10.0000"))
        val trigger = fixedTrigger(100L, TriggerType.STOP_LOSS, BigDecimal("95.0000"), BigDecimal("100.0000"))
        every { triggerRepository.findByTickerAndState("005930", TriggerState.ARMED) } returns listOf(trigger)
        every { positionRepository.findByIdWithLock(10L) } returns Optional.of(position)
        every { triggerRepository.findArmedGroupForUpdate("005930", 10L, TriggerType.STOP_LOSS) } returns listOf(trigger)
        every { orderRepository.existsOpenOrderByAccountTickerSideWithoutGroup(1L, "005930") } returns true
        every { triggerRepository.saveAll(any<List<PositionExitTrigger>>()) } answers { firstArg() }

        orchestrator.onQuote("005930", BigDecimal("94.0000"), quoteAt)

        assertEquals(TriggerState.SKIPPED, trigger.state)
        assertEquals(TriggerSkipReason.MANUAL_SELL_CONFLICT, trigger.skipReason)
        verify(exactly = 0) {
            orderCommandService.createGroupedAutoExitSellOrder(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `duplicate grouped order is treated as triggered when existing order is found`() {
        val position = position(quantity = BigDecimal("10.0000"))
        val trigger = fixedTrigger(100L, TriggerType.STOP_LOSS, BigDecimal("95.0000"), BigDecimal("100.0000"))
        every { triggerRepository.findByTickerAndState("005930", TriggerState.ARMED) } returns listOf(trigger)
        every { positionRepository.findByIdWithLock(10L) } returns Optional.of(position)
        every { triggerRepository.findArmedGroupForUpdate("005930", 10L, TriggerType.STOP_LOSS) } returns listOf(trigger)
        every { orderRepository.existsOpenOrderByAccountTickerSideWithoutGroup(1L, "005930") } returns false
        every { orderRepository.sumOpenSellQuantity(1L, "005930") } returns BigDecimal.ZERO
        every { orderCommandService.createGroupedAutoExitSellOrder(any(), any(), any(), any(), any(), any()) } throws
            DataIntegrityViolationException("uq")
        every { orderRepository.findByAccountIdAndOrderGroupId(1L, any()) } returns mockk<Order>()
        every { triggerRepository.saveAll(any<List<PositionExitTrigger>>()) } answers { firstArg() }

        orchestrator.onQuote("005930", BigDecimal("94.0000"), quoteAt)

        assertEquals(TriggerState.TRIGGERED, trigger.state)
        verify(exactly = 0) { notificationEventPublisher.publishOrderError(any(), any(), any(), any()) }
    }

    @Test
    fun `order creation failure marks fired triggers failed and publishes notification`() {
        val position = position(quantity = BigDecimal("10.0000"))
        val trigger = fixedTrigger(100L, TriggerType.STOP_LOSS, BigDecimal("95.0000"), BigDecimal("100.0000"))
        every { triggerRepository.findByTickerAndState("005930", TriggerState.ARMED) } returns listOf(trigger)
        every { positionRepository.findByIdWithLock(10L) } returns Optional.of(position)
        every { triggerRepository.findArmedGroupForUpdate("005930", 10L, TriggerType.STOP_LOSS) } returns listOf(trigger)
        every { orderRepository.existsOpenOrderByAccountTickerSideWithoutGroup(1L, "005930") } returns false
        every { orderRepository.sumOpenSellQuantity(1L, "005930") } returns BigDecimal.ZERO
        every { orderCommandService.createGroupedAutoExitSellOrder(any(), any(), any(), any(), any(), any()) } throws
            IllegalStateException("broker unavailable")
        every { triggerRepository.saveAll(any<List<PositionExitTrigger>>()) } answers { firstArg() }

        orchestrator.onQuote("005930", BigDecimal("94.0000"), quoteAt)

        assertEquals(TriggerState.FAILED, trigger.state)
        verify(exactly = 1) {
            notificationEventPublisher.publishOrderError(
                1L,
                null,
                match { it.contains("positionId=10") && it.contains("triggerType=STOP_LOSS") },
                any(),
            )
        }
    }

    @Test
    fun `lock conflict skips armed candidates`() {
        val trigger = fixedTrigger(100L, TriggerType.STOP_LOSS, BigDecimal("95.0000"), BigDecimal("100.0000"))
        every { triggerRepository.findByTickerAndState("005930", TriggerState.ARMED) } returns listOf(trigger)
        every { positionRepository.findByIdWithLock(10L) } throws PessimisticLockingFailureException("lock")
        every { triggerRepository.saveAll(any<List<PositionExitTrigger>>()) } answers { firstArg() }

        orchestrator.onQuote("005930", BigDecimal("94.0000"), quoteAt)

        assertEquals(TriggerState.SKIPPED, trigger.state)
        assertEquals(TriggerSkipReason.LOCK_CONFLICT, trigger.skipReason)
    }

    private fun stubSuccessfulOrder(
        position: Position,
        triggers: List<PositionExitTrigger>,
        quantity: io.mockk.CapturingSlot<BigDecimal>,
        orderGroupId: io.mockk.CapturingSlot<String>,
        audits: io.mockk.CapturingSlot<List<AutoExitTriggerAuditInput>>,
    ) {
        every { triggerRepository.findByTickerAndState("005930", TriggerState.ARMED) } returns triggers
        every { positionRepository.findByIdWithLock(10L) } returns Optional.of(position)
        every { triggerRepository.findArmedGroupForUpdate("005930", 10L, TriggerType.STOP_LOSS) } returns triggers
        every { orderRepository.existsOpenOrderByAccountTickerSideWithoutGroup(1L, "005930") } returns false
        every { orderRepository.sumOpenSellQuantity(1L, "005930") } returns BigDecimal.ZERO
        every {
            orderCommandService.createGroupedAutoExitSellOrder(
                accountId = 1L,
                ticker = "005930",
                marketType = MarketType.KOSPI,
                quantity = capture(quantity),
                orderGroupId = capture(orderGroupId),
                triggerAuditInputs = capture(audits),
            )
        } returns mockk<Order>()
        every { triggerRepository.saveAll(any<List<PositionExitTrigger>>()) } answers { firstArg() }
    }

    private fun position(quantity: BigDecimal): Position {
        val account = Account.create("a", AccountType.STOCK, TradingMode.LOCAL, BigDecimal("1000.0000")).withId(1L)
        return Position.createWithHolding(
            account = account,
            ticker = "005930",
            marketType = MarketType.KOSPI,
            quantity = quantity,
            avgBuyPrice = BigDecimal("100.0000"),
        ).withId(10L)
    }

    private fun fixedTrigger(
        id: Long,
        type: TriggerType,
        price: BigDecimal,
        exitRatioPercent: BigDecimal,
    ): PositionExitTrigger =
        PositionExitTrigger.create(
            positionId = 10L,
            accountId = 1L,
            ticker = "005930",
            triggerType = type,
            triggerPercent = null,
            triggerPrice = price,
            priceBasisPolicy = PriceBasisPolicy.FIXED_PRICE,
            exitRatioPercent = exitRatioPercent,
        ).withId(id)
}
