package com.papertrading.api.application.order

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
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
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
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

    private fun buyOrder(account: Account, qty: String, limitPrice: String, locked: String): Order = Order.create(
        account = account,
        ticker = "005930",
        marketType = MarketType.KOSPI,
        orderType = OrderType.LIMIT,
        orderSide = OrderSide.BUY,
        orderCondition = OrderCondition.DAY,
        quantity = BigDecimal(qty),
        limitPrice = BigDecimal(limitPrice),
        lockedAmount = BigDecimal(locked),
        idempotencyKey = "buy-key-1",
    ).withId(10L)

    /** 주문 접수 때 잠금이 걸린 KIS_PAPER 매수 계좌·주문(기본 10@70000, 잠금 700000)과, 저장된 원장 수집 목록 */
    private fun setupBuyFills(
        qty: String = "10",
        limitPrice: String = "70000",
        locked: String = "700000",
    ): Triple<Account, Order, MutableList<AccountLedger>> {
        val account = account(TradingMode.KIS_PAPER)
        val order = buyOrder(account, qty, limitPrice, locked)
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

    private fun warnLogsOf(block: () -> Unit): List<String> {
        val logger = LoggerFactory.getLogger(ExecutionProcessor::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().also { it.start() }
        logger.addAppender(appender)
        try {
            block()
        } finally {
            logger.detachAppender(appender)
        }
        return appender.list.filter { it.level == Level.WARN }.map { it.formattedMessage }
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

    @Test
    fun `KIS_PAPER 매수 소수 수량 단일 체결도 잠금 계산식 그대로 체결되고 잠금 0이다`() {
        val (account, order, _) = setupBuyFills(qty = "0.3", limitPrice = "100.0005", locked = "30.00015")

        fillBuy("100.0005", "0.3", "ext-1")

        assertEquals(OrderStatus.FILLED, order.orderStatus)
        assertSameAmount("0", account.lockedDeposit, "lockedDeposit")
    }

    @Test
    fun `KIS_PAPER 매수 극소 수량 체결도 체결 금액이 0이 되지 않아 체결되고 잠금 0이다`() {
        val (account, order, _) = setupBuyFills(qty = "0.00000001", limitPrice = "1234.5678", locked = "0.0001")

        fillBuy("1234.5678", "0.00000001", "ext-1")

        assertEquals(OrderStatus.FILLED, order.orderStatus)
        assertSameAmount("0", account.lockedDeposit, "lockedDeposit")
    }

    @Test
    fun `KIS_PAPER 매수 소수 수량 단일 체결의 주문 평균가는 체결가와 같다`() {
        val (_, order, _) = setupBuyFills(qty = "0.12345678", limitPrice = "72000", locked = "9000")

        fillBuy("70000.1234", "0.12345678", "ext-1")

        assertSameAmount("70000.1234", order.avgFilledPrice!!, "avgFilledPrice")
    }

    @Test
    fun `KIS_PAPER 매수 남은 수량보다 많이 체결하면 거부하고 저장 0이며 다른 주문 잠금을 안 쓴다`() {
        val (account, order, ledgers) = setupBuyFills()
        account.lockDeposit(BigDecimal("200000"))
        fillBuy("70000", "4", "ext-1")
        val ledgerCountBefore = ledgers.size

        assertThrows(IllegalArgumentException::class.java) { fillBuy("70000", "7", "ext-2") }

        assertEquals(ledgerCountBefore, ledgers.size)
        verify(exactly = 1) { executionRepository.save(any()) }
        assertSameAmount("4", order.filledQuantity, "filledQuantity")
        assertSameAmount("620000", account.lockedDeposit, "lockedDeposit")
    }

    @Test
    fun `LOCAL 매도 소수 수량은 원장 매도 금액과 예수금 입금을 반올림 없이 기록한다`() {
        val account = account(TradingMode.LOCAL)
        val order = sellOrder(account, qty = BigDecimal("0.3"))
        setupCommonMocks(account, order)
        val ledgers = mutableListOf<AccountLedger>()
        every { executionRepository.save(any()) } answers { firstArg<Execution>().withId(100L) }
        every { accountLedgerRepository.save(any()) } answers { firstArg<AccountLedger>().also { ledgers.add(it) } }

        processor.fillLocal(10L, BigDecimal("100.0005"), BigDecimal("0.3"))

        // 100.0005 * 0.3 = 30.00015
        val sell = ledgers.single { it.transactionType == TransactionType.SELL_EXECUTE }
        assertSameAmount("30.00015", sell.amount, "SELL_EXECUTE amount")
        assertSameAmount("1000030.00015", account.availableDeposit, "availableDeposit")
    }

    @Test
    fun `이미 끝난 주문에 새 체결이 오면 저장 없이 무시하고 경고 로그를 남긴다`() {
        val (_, order, ledgers) = setupBuyFills()
        fillBuy("70000", "10", "ext-1")
        val ledgerCountBefore = ledgers.size

        val warns = warnLogsOf { fillBuy("70000", "1", "ext-extra") }

        assertEquals(ledgerCountBefore, ledgers.size)
        assertSameAmount("10", order.filledQuantity, "filledQuantity")
        assertTrue(
            warns.any { "orderId=10" in it && "ext-extra" in it && "FILLED" in it },
            "경고 로그에 주문 id·체결 id·상태가 있어야 함: $warns",
        )
    }

    @Test
    fun `잠금보다 체결 금액이 커서 해제액을 0으로 깎으면 경고 로그를 남긴다`() {
        val (account, _, ledgers) = setupBuyFills()
        account.lockDeposit(BigDecimal("200000"))

        val warns = warnLogsOf { fillBuy("71000", "10", "ext-1") }

        assertEquals(0, ledgers.count { it.transactionType == TransactionType.BUY_UNLOCK })
        assertTrue(warns.any { "orderId=10" in it && "잠금" in it }, "경고 로그에 주문 id·잠금 초과가 있어야 함: $warns")
    }

    @Test
    fun `잠금 안에서 체결이 끝나면 경고 로그를 남기지 않는다`() {
        val (_, _, _) = setupBuyFills()

        val warns = warnLogsOf { fillBuy("70000", "10", "ext-1") }

        assertEquals(emptyList<String>(), warns)
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
