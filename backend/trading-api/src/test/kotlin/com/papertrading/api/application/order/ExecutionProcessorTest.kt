package com.papertrading.api.application.order

import com.papertrading.api.application.notification.SlackNotificationEventPublisher
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.account.AccountLedger
import com.papertrading.api.domain.entity.feepolicy.FeePolicy
import com.papertrading.api.domain.entity.order.Execution
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.entity.settlement.Settlement
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.enums.OrderType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.enums.TransactionType
import com.papertrading.api.domain.event.ExecutionFilledEvent
import com.papertrading.api.domain.port.CollectorSubscriptionPort
import com.papertrading.api.infrastructure.persistence.AccountLedgerRepository
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.ExecutionRepository
import com.papertrading.api.infrastructure.persistence.FeePolicyRepository
import com.papertrading.api.infrastructure.persistence.OrderRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import com.papertrading.api.infrastructure.persistence.SettlementExecutionRepository
import com.papertrading.api.infrastructure.persistence.SettlementRepository
import com.papertrading.api.support.withId
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.time.Instant
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

    private fun account(
        tradingMode: TradingMode,
        deposit: BigDecimal = BigDecimal("1000000"),
    ): Account = Account.create(
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

    private fun buyOrder(account: Account): Order = Order.create(
        account = account,
        ticker = "005930",
        marketType = MarketType.KOSPI,
        orderType = OrderType.LIMIT,
        orderSide = OrderSide.BUY,
        orderCondition = OrderCondition.DAY,
        quantity = BigDecimal("10"),
        limitPrice = BigDecimal("70000"),
        lockedAmount = BigDecimal("700000"),
        idempotencyKey = "buy-key-1",
    ).withId(10L)

    /** 주문 접수 때 700000 잠금이 걸린 KIS_PAPER 매수 계좌·주문과, 저장된 원장 수집 목록 */
    private fun setupBuyFills(): Triple<Account, Order, MutableList<AccountLedger>> {
        val account = account(TradingMode.KIS_PAPER)
        val order = buyOrder(account)
        account.lockDeposit(order.lockedAmount)
        val ledgers = mutableListOf<AccountLedger>()
        var executionSeq = 100L
        every { orderRepository.findByIdWithOptimisticLock(10L) } returns Optional.of(order)
        every { executionRepository.findByExternalExecutionId(any()) } returns Optional.empty()
        every { executionRepository.save(any()) } answers { firstArg<Execution>().withId(executionSeq++) }
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { feePolicyRepository.findActivePolicy(any(), any(), any()) } returns Optional.empty()
        every { positionRepository.findByAccountIdAndTickerWithLock(1L, "005930") } returns Optional.empty()
        every { positionRepository.save(any()) } answers { firstArg() }
        every { accountLedgerRepository.save(any()) } answers { firstArg<AccountLedger>().also { ledgers.add(it) } }
        every { orderRepository.findByAccountIdAndOrderStatusIn(any(), any()) } returns emptyList()
        every { collectorSubscriptionPort.unsubscribe(any(), any()) } just runs
        return Triple(account, order, ledgers)
    }

    private fun fillBuy(price: String, qty: String, externalId: String) {
        processor.fillExternal(
            orderId = 10L,
            fillPrice = BigDecimal(price),
            fillQty = BigDecimal(qty),
            externalExecutionId = externalId,
            executedAt = Instant.parse("2026-11-15T01:00:00Z"),
        )
    }

    private fun assertSameAmount(expected: String, actual: BigDecimal, name: String) {
        assertEquals(0, BigDecimal(expected).compareTo(actual), "$name: expected $expected but was $actual")
    }

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
    fun `LOCAL SELL — availableDeposit 증가 and settlement tax is feePolicy 기반`() {
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

        processor.fillLocal(10L, fillPrice, fillQty)

        // LOCAL: 즉시 입금 — netProceeds = 70000 * 5 - 0(fee) = 350000
        val expectedNetProceeds = fillPrice.multiply(fillQty) // fee=0
        assert(account.availableDeposit > initialDeposit) {
            "availableDeposit should increase for LOCAL SELL"
        }
        assertEquals(
            0,
            initialDeposit.add(expectedNetProceeds).compareTo(account.availableDeposit),
            "availableDeposit should be ${initialDeposit.add(expectedNetProceeds)} but was ${account.availableDeposit}",
        )

        assertEquals(0, BigDecimal.ZERO.compareTo(settlementSlot.captured.tax))
    }

    @Test
    fun `KIS_LIVE SELL — availableDeposit 불변, 정산 1건 저장`() {
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

        processor.fillLocal(10L, fillPrice, fillQty)

        // KIS: 즉시 입금 없음 — availableDeposit 변화 없어야 함
        // devkit:test-change 원장 #552 BigDecimal 단언을 compareTo 로 — 사람 승인 계획 2026-11-15
        assertSameAmount(initialDeposit.toPlainString(), account.availableDeposit, "availableDeposit")

        verify(exactly = 1) { settlementRepository.save(any()) }
    }

    @Test
    fun `KIS_PAPER SELL — availableDeposit 불변, 정산 1건 저장`() {
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

        processor.fillLocal(10L, fillPrice, fillQty)

        // devkit:test-change 원장 #552 BigDecimal 단언을 compareTo 로 — 사람 승인 계획 2026-11-15
        assertSameAmount(initialDeposit.toPlainString(), account.availableDeposit, "availableDeposit")
        verify(exactly = 1) { settlementRepository.save(any()) }
    }

    @Test
    fun `KIS_PAPER 매수 가격이 다른 두 번 체결하면 잠금 0 가용 304000 주문 누적 696000이다`() {
        val (account, order, ledgers) = setupBuyFills()

        fillBuy("69000", "4", "ext-1")
        fillBuy("70000", "6", "ext-2")

        assertEquals(OrderStatus.FILLED, order.orderStatus)
        assertSameAmount("696000", order.totalAmount!!, "order.totalAmount")
        assertSameAmount("0", account.lockedDeposit, "lockedDeposit")
        assertSameAmount("304000", account.availableDeposit, "availableDeposit")
        assertSameAmount("304000", account.deposit, "deposit")
        val unlocks = ledgers.filter { it.transactionType == TransactionType.BUY_UNLOCK }
        assertEquals(1, unlocks.size)
        assertSameAmount("4000", unlocks.single().amount, "BUY_UNLOCK amount")
    }

    @Test
    fun `KIS_PAPER 매수 같은 가격 두 번 체결하면 예외 없이 잠금 0이고 해제 원장이 없다`() {
        val (account, order, ledgers) = setupBuyFills()

        assertDoesNotThrow {
            fillBuy("70000", "4", "ext-1")
            fillBuy("70000", "6", "ext-2")
        }

        assertEquals(OrderStatus.FILLED, order.orderStatus)
        assertSameAmount("0", account.lockedDeposit, "lockedDeposit")
        assertSameAmount("300000", account.availableDeposit, "availableDeposit")
        assertEquals(0, ledgers.count { it.transactionType == TransactionType.BUY_UNLOCK })
    }

    @Test
    fun `KIS_PAPER 매수 단일 체결이 지정가보다 싸면 차액 20000을 해제한다`() {
        val (account, order, ledgers) = setupBuyFills()

        fillBuy("68000", "10", "ext-1")

        assertEquals(OrderStatus.FILLED, order.orderStatus)
        assertSameAmount("0", account.lockedDeposit, "lockedDeposit")
        assertSameAmount("320000", account.availableDeposit, "availableDeposit")
        val unlocks = ledgers.filter { it.transactionType == TransactionType.BUY_UNLOCK }
        assertEquals(1, unlocks.size)
        assertSameAmount("20000", unlocks.single().amount, "BUY_UNLOCK amount")
    }

    /** Spec Test 1C: fill() 뒤 ExecutionFilledEvent 발행(ticker, side, executionId 일치) */
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

        processor.fillLocal(10L, fillPrice, fillQty)

        verify(exactly = 1) { eventPublisher.publishEvent(any<ExecutionFilledEvent>()) }

        val publishedEvent = eventSlot.captured
        assertEquals(300L, publishedEvent.executionId)
        assertEquals(10L, publishedEvent.orderId)
        assertEquals("005930", publishedEvent.ticker)
        assertEquals(OrderSide.SELL, publishedEvent.side)
    }
}
