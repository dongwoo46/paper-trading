package com.papertrading.api.application.order

import com.papertrading.api.application.notification.SlackNotificationEventPublisher
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.enums.OrderType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.support.withId
import com.papertrading.api.domain.entity.order.Execution
import com.papertrading.api.domain.entity.feepolicy.FeePolicy
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.port.CollectorSubscriptionPort
import com.papertrading.api.infrastructure.persistence.AccountLedgerRepository
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.ExecutionRepository
import com.papertrading.api.infrastructure.persistence.FeePolicyRepository
import com.papertrading.api.infrastructure.persistence.OrderRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import com.papertrading.api.infrastructure.persistence.SettlementExecutionRepository
import com.papertrading.api.infrastructure.persistence.SettlementRepository
import com.papertrading.api.domain.entity.settlement.Settlement
import com.papertrading.api.domain.event.ExecutionFilledEvent
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.util.Optional

class ExecutionProcessorTest {

    private val orderRepository = mockk<OrderRepository>()
    private val accountRepository = mockk<AccountRepository>()
    private val positionRepository = mockk<PositionRepository>()
    private val executionRepository = mockk<ExecutionRepository>()
    private val accountLedgerRepository = mockk<AccountLedgerRepository>()
    private val feePolicyRepository = mockk<FeePolicyRepository>()
    private val collectorSubscriptionPort = mockk<CollectorSubscriptionPort>()
    private val settlementRepository = mockk<SettlementRepository>()
    private val settlementExecutionRepository = mockk<SettlementExecutionRepository>()
    private val eventPublisher = mockk<ApplicationEventPublisher>().also {
        justRun { it.publishEvent(any<Any>()) }
    }
    private val slackNotificationEventPublisher = mockk<SlackNotificationEventPublisher>().also {
        justRun { it.publishExecutionFilled(any(), any(), any(), any(), any()) }
    }

    private val processor = ExecutionProcessor(
        orderRepository = orderRepository,
        accountRepository = accountRepository,
        positionRepository = positionRepository,
        executionRepository = executionRepository,
        accountLedgerRepository = accountLedgerRepository,
        feePolicyRepository = feePolicyRepository,
        collectorSubscriptionPort = collectorSubscriptionPort,
        settlementRepository = settlementRepository,
        settlementExecutionRepository = settlementExecutionRepository,
        eventPublisher = eventPublisher,
        slackNotificationEventPublisher = slackNotificationEventPublisher,
    )

    private fun account(tradingMode: TradingMode, deposit: BigDecimal = BigDecimal("1000000")): Account =
        Account.create(
            accountName = "test",
            accountType = AccountType.STOCK,
            tradingMode = tradingMode,
            initialDeposit = deposit,
        ).withId(1L)

    private fun sellOrder(account: Account, qty: BigDecimal = BigDecimal("5")): Order = Order.create(
        account = account,
        ticker = "005930",
        marketType = MarketType.KOSPI,
        orderType = OrderType.LIMIT,
        orderSide = OrderSide.SELL,
        orderCondition = OrderCondition.DAY,
        quantity = qty,
        limitPrice = BigDecimal("70000"),
        lockedAmount = BigDecimal.ZERO,
        idempotencyKey = "sell-key-1",
    ).withId(10L)

    private fun setupCommonMocks(account: Account, order: Order) {
        val position = Position.create(account = account, ticker = "005930", marketType = MarketType.KOSPI).apply {
            applyBuy(order.quantity, BigDecimal("60000"))
            lockQuantity(order.quantity)
        }.withId(99L)
        every { orderRepository.findByIdWithOptimisticLock(10L) } returns Optional.of(order)
        every { executionRepository.findByExternalExecutionId(any()) } returns Optional.empty()
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { feePolicyRepository.findActivePolicy(any(), any(), any()) } returns Optional.empty()
        every { positionRepository.findByAccountIdAndTickerWithLock(1L, "005930") } returns Optional.of(position)
        every { positionRepository.save(any()) } answers { firstArg() }
        every { positionRepository.applySellExecutionAtomically(any(), any()) } returns 1
        every { accountLedgerRepository.save(any()) } answers { firstArg() }
        every { orderRepository.findByAccountIdAndOrderStatusIn(any(), any()) } returns emptyList()
        every { collectorSubscriptionPort.unsubscribe(any(), any()) } just runs
        // Settlement 관련 (FILLED 시 항상 생성)
        every { settlementRepository.save(any()) } answers { firstArg() }
        every { settlementExecutionRepository.save(any()) } answers { firstArg() }
    }

    @Test
    fun `LOCAL SELL — availableDeposit 증가 and settlement tax is 0_2pct`() {
        val account = account(TradingMode.LOCAL)
        val order = sellOrder(account, qty = BigDecimal("5"))
        val fillPrice = BigDecimal("70000")
        val fillQty = BigDecimal("5")

        val settlementSlot = slot<Settlement>()
        setupCommonMocks(account, order)
        every { executionRepository.save(any()) } answers {
            firstArg<Execution>().withId(100L)
        }
        every { settlementRepository.save(capture(settlementSlot)) } answers { firstArg() }

        val initialDeposit = account.availableDeposit

        processor.fill(10L, fillPrice, fillQty)

        // LOCAL: 즉시 입금 — netProceeds = 70000 * 5 - 0(fee) = 350000
        val expectedNetProceeds = fillPrice.multiply(fillQty) // fee=0
        assert(account.availableDeposit > initialDeposit) {
            "availableDeposit should increase for LOCAL SELL"
        }
        assertEquals(
            0,
            initialDeposit.add(expectedNetProceeds).compareTo(account.availableDeposit),
            "availableDeposit should be ${initialDeposit.add(expectedNetProceeds)} but was ${account.availableDeposit}"
        )

        assertEquals(0, BigDecimal("700.0000").compareTo(settlementSlot.captured.tax))
    }

    @Test
    fun `KIS_LIVE SELL — availableDeposit 불변, no receivable settlement creation`() {
        val account = account(TradingMode.KIS_LIVE)
        val order = sellOrder(account, qty = BigDecimal("5"))
        val fillPrice = BigDecimal("70000")
        val fillQty = BigDecimal("5")

        setupCommonMocks(account, order)
        every { executionRepository.save(any()) } answers {
            firstArg<Execution>().withId(200L)
        }
        every { settlementRepository.save(any()) } answers { firstArg() }
        every { settlementExecutionRepository.save(any()) } answers { firstArg() }

        val initialDeposit = account.availableDeposit

        processor.fill(10L, fillPrice, fillQty)

        // KIS: 즉시 입금 없음 — availableDeposit 변화 없어야 함
        assertEquals(initialDeposit, account.availableDeposit)

        verify(exactly = 1) { settlementRepository.save(any()) }
    }

    @Test
    fun `KIS_PAPER SELL — availableDeposit 불변, no receivable settlement creation`() {
        val account = account(TradingMode.KIS_PAPER)
        val order = sellOrder(account, qty = BigDecimal("5"))
        val fillPrice = BigDecimal("70000")
        val fillQty = BigDecimal("5")

        setupCommonMocks(account, order)
        every { executionRepository.save(any()) } answers {
            firstArg<Execution>().withId(201L)
        }
        every { settlementRepository.save(any()) } answers { firstArg() }
        every { settlementExecutionRepository.save(any()) } answers { firstArg() }

        val initialDeposit = account.availableDeposit

        processor.fill(10L, fillPrice, fillQty)

        assertEquals(initialDeposit, account.availableDeposit)
        verify(exactly = 1) { settlementRepository.save(any()) }
    }

    /** Spec Test 1C: verify publishEvent(ExecutionFilledEvent) called after fill() with correct ticker, side, executionId */
    @Test
    fun `fill publishes ExecutionFilledEvent with correct ticker, side, and executionId`() {
        val account = account(TradingMode.LOCAL)
        val order = sellOrder(account, qty = BigDecimal("5"))
        val fillPrice = BigDecimal("70000")
        val fillQty = BigDecimal("5")

        setupCommonMocks(account, order)
        every { executionRepository.save(any()) } answers {
            firstArg<Execution>().withId(300L)
        }

        val eventSlot = slot<ExecutionFilledEvent>()
        justRun { eventPublisher.publishEvent(capture(eventSlot)) }

        processor.fill(10L, fillPrice, fillQty)

        verify(exactly = 1) { eventPublisher.publishEvent(any<ExecutionFilledEvent>()) }

        val publishedEvent = eventSlot.captured
        assertEquals(300L, publishedEvent.executionId)
        assertEquals(10L, publishedEvent.orderId)
        assertEquals("005930", publishedEvent.ticker)
        assertEquals(OrderSide.SELL, publishedEvent.side)
    }
}

