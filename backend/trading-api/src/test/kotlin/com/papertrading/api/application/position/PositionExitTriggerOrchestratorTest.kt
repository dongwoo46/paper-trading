package com.papertrading.api.application.position

import com.papertrading.api.application.notification.SlackNotificationEventPublisher
import com.papertrading.api.application.order.OrderCommandService
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.entity.position.PositionExitTrigger
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
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
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

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

    @Test
    fun `중복 quote 이벤트에서도 동일 트리거 주문은 한번만 생성`() {
        val account = Account.create("a", AccountType.STOCK, TradingMode.LOCAL, BigDecimal("1000")).withId(1L)
        val position = Position.createWithHolding(account, "005930", MarketType.KOSPI, BigDecimal("2"), BigDecimal("100")).withId(10L)
        val trigger = PositionExitTrigger.create(10L, 1L, "005930", true, BigDecimal("3"), BigDecimal("7"))

        every { positionRepository.findByTickerAndQuantityGreaterThan("005930", BigDecimal.ZERO) } returns listOf(position)
        every { triggerRepository.findByPositionIdForUpdate(10L) } returnsMany listOf(trigger, trigger)
        every { positionRepository.findByIdWithLock(10L) } returns Optional.of(position)
        every { triggerRepository.save(any()) } answers { firstArg() }
        every { orderCommandService.createAutoExitSellOrder(any(), any(), any()) } returns mockk<Order>()
        every { orderRepository.findByAccountIdAndIdempotencyKey(any(), any()) } returns null

        orchestrator.onQuote("005930", BigDecimal("97"), Instant.now())
        orchestrator.onQuote("005930", BigDecimal("97"), Instant.now())

        verify(exactly = 1) { orderCommandService.createAutoExitSellOrder(any(), any(), any()) }
    }

    @Test
    fun `동시 트리거 시도에서도 주문 생성은 최대 1회여야 한다`() {
        val account = Account.create("a", AccountType.STOCK, TradingMode.LOCAL, BigDecimal("1000")).withId(1L)
        val position = Position.createWithHolding(account, "005930", MarketType.KOSPI, BigDecimal("2"), BigDecimal("100")).withId(10L)
        val trigger = PositionExitTrigger.create(10L, 1L, "005930", true, BigDecimal("3"), BigDecimal("7"))
        val barrier = CountDownLatch(2)
        val orderCallCount = AtomicInteger(0)

        every { positionRepository.findByTickerAndQuantityGreaterThan("005930", BigDecimal.ZERO) } returns listOf(position)
        every { positionRepository.findByIdWithLock(10L) } returns Optional.of(position)
        every { triggerRepository.findByPositionIdForUpdate(10L) } answers {
            barrier.countDown()
            barrier.await(1, TimeUnit.SECONDS)
            trigger
        }
        every { triggerRepository.save(any()) } answers { firstArg() }
        every { orderCommandService.createAutoExitSellOrder(any(), any(), any()) } answers {
            orderCallCount.incrementAndGet()
            mockk<Order>()
        }
        every { orderRepository.findByAccountIdAndIdempotencyKey(any(), any()) } returns null

        val executor = Executors.newFixedThreadPool(2)
        try {
            val tasks = listOf(
                Callable { orchestrator.onQuote("005930", BigDecimal("97"), Instant.now()) },
                Callable { orchestrator.onQuote("005930", BigDecimal("97"), Instant.now()) },
            )
            executor.invokeAll(tasks)
        } finally {
            executor.shutdown()
            executor.awaitTermination(2, TimeUnit.SECONDS)
        }

        assertTrue(orderCallCount.get() <= 1, "동시 처리 시 중복 주문이 생성되면 안 됩니다.")
    }

    @Test
    fun `중복키 발생 시 기존 멱등 주문이 확인되면 TRIGGERED로 처리`() {
        val account = Account.create("a", AccountType.STOCK, TradingMode.LOCAL, BigDecimal("1000")).withId(1L)
        val position = Position.createWithHolding(account, "005930", MarketType.KOSPI, BigDecimal("2"), BigDecimal("100")).withId(10L)
        val trigger = PositionExitTrigger.create(10L, 1L, "005930", true, BigDecimal("3"), BigDecimal("7"))
        val order = mockk<Order>()
        val quoteAt = Instant.parse("2026-05-08T12:00:00Z")

        every { positionRepository.findByTickerAndQuantityGreaterThan("005930", BigDecimal.ZERO) } returns listOf(position)
        every { triggerRepository.findByPositionIdForUpdate(10L) } returns trigger
        every { positionRepository.findByIdWithLock(10L) } returns Optional.of(position)
        every { triggerRepository.save(any()) } answers { firstArg() }
        every { orderCommandService.createAutoExitSellOrder(any(), any(), any()) } throws DataIntegrityViolationException("uq")
        every {
            orderRepository.findByAccountIdAndIdempotencyKey(
                1L,
                "auto-exit:10:0:${TriggerType.STOP_LOSS.name}"
            )
        } returns order

        orchestrator.onQuote("005930", BigDecimal("97"), quoteAt)

        assertEquals(TriggerState.TRIGGERED, trigger.stopLossState)
        verify(exactly = 0) { notificationEventPublisher.publishOrderError(any(), any(), any(), any()) }
    }

    @Test
    fun `중복키 발생 후 기존 주문 미확인 시 FAILED 처리 및 알림 발행`() {
        val account = Account.create("a", AccountType.STOCK, TradingMode.LOCAL, BigDecimal("1000")).withId(1L)
        val position = Position.createWithHolding(account, "005930", MarketType.KOSPI, BigDecimal("2"), BigDecimal("100")).withId(10L)
        val trigger = PositionExitTrigger.create(10L, 1L, "005930", true, BigDecimal("3"), BigDecimal("7"))
        val quoteAt = Instant.parse("2026-05-08T12:00:00Z")
        val msg = slot<String>()

        every { positionRepository.findByTickerAndQuantityGreaterThan("005930", BigDecimal.ZERO) } returns listOf(position)
        every { triggerRepository.findByPositionIdForUpdate(10L) } returns trigger
        every { positionRepository.findByIdWithLock(10L) } returns Optional.of(position)
        every { triggerRepository.save(any()) } answers { firstArg() }
        every { orderCommandService.createAutoExitSellOrder(any(), any(), any()) } throws DataIntegrityViolationException("uq")
        every {
            orderRepository.findByAccountIdAndIdempotencyKey(
                1L,
                "auto-exit:10:0:${TriggerType.STOP_LOSS.name}"
            )
        } returns null
        every { notificationEventPublisher.publishOrderError(1L, null, capture(msg), any()) } returns Unit

        orchestrator.onQuote("005930", BigDecimal("97"), quoteAt)

        assertEquals(TriggerState.FAILED, trigger.stopLossState)
        assertTrue(msg.captured.contains("positionId=10"))
        assertTrue(msg.captured.contains("idempotencyKey=auto-exit:10:0:STOP_LOSS"))
        verify(exactly = 1) { notificationEventPublisher.publishOrderError(1L, null, any(), any()) }
    }

    @Test
    fun `트리거 조건 도달 후 최신 포지션이 이미 매도 잠금이면 SKIPPED로 완료`() {
        val account = Account.create("a", AccountType.STOCK, TradingMode.LOCAL, BigDecimal("1000")).withId(1L)
        val stalePosition = Position.createWithHolding(account, "005930", MarketType.KOSPI, BigDecimal("2"), BigDecimal("100")).withId(10L)
        val lockedPosition = Position.createWithHolding(
            account = account,
            ticker = "005930",
            marketType = MarketType.KOSPI,
            quantity = BigDecimal("2"),
            avgBuyPrice = BigDecimal("100"),
            lockedQuantity = BigDecimal("2"),
        ).withId(10L)
        val trigger = PositionExitTrigger.create(10L, 1L, "005930", true, BigDecimal("3"), BigDecimal("7"))
        val quoteAt = Instant.parse("2026-05-08T12:00:00Z")

        every { positionRepository.findByTickerAndQuantityGreaterThan("005930", BigDecimal.ZERO) } returns listOf(stalePosition)
        every { triggerRepository.findByPositionIdForUpdate(10L) } returns trigger
        every { positionRepository.findByIdWithLock(10L) } returns Optional.of(lockedPosition)
        every { triggerRepository.save(any()) } answers { firstArg() }

        orchestrator.onQuote("005930", BigDecimal("97"), quoteAt)

        assertEquals(TriggerState.SKIPPED, trigger.stopLossState)
        assertEquals(TriggerState.CANCELED, trigger.takeProfitState)
        assertEquals(TriggerSkipReason.SELL_ALREADY_LOCKED, trigger.skipReason)
        verify(exactly = 0) { orderCommandService.createAutoExitSellOrder(any(), any(), any()) }
    }

    @Test
    fun `트리거 조건 도달 후 최신 포지션이 없으면 POSITION_CLOSED로 SKIPPED 완료`() {
        val account = Account.create("a", AccountType.STOCK, TradingMode.LOCAL, BigDecimal("1000")).withId(1L)
        val stalePosition = Position.createWithHolding(account, "005930", MarketType.KOSPI, BigDecimal("2"), BigDecimal("100")).withId(10L)
        val trigger = PositionExitTrigger.create(10L, 1L, "005930", true, BigDecimal("3"), BigDecimal("7"))
        val quoteAt = Instant.parse("2026-05-08T12:00:00Z")

        every { positionRepository.findByTickerAndQuantityGreaterThan("005930", BigDecimal.ZERO) } returns listOf(stalePosition)
        every { triggerRepository.findByPositionIdForUpdate(10L) } returns trigger
        every { positionRepository.findByIdWithLock(10L) } returns Optional.empty()
        every { triggerRepository.save(any()) } answers { firstArg() }

        orchestrator.onQuote("005930", BigDecimal("97"), quoteAt)

        assertEquals(TriggerState.SKIPPED, trigger.stopLossState)
        assertEquals(TriggerState.CANCELED, trigger.takeProfitState)
        assertEquals(TriggerSkipReason.POSITION_CLOSED, trigger.skipReason)
        verify(exactly = 0) { orderCommandService.createAutoExitSellOrder(any(), any(), any()) }
    }
}
