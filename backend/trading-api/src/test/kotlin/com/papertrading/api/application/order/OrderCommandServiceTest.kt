package com.papertrading.api.application.order

import com.papertrading.api.application.order.command.PlaceOrderCommand
import com.papertrading.api.common.exception.BrokerOrderNotSupportedException
import com.papertrading.api.common.exception.PositionNotFoundException
import com.papertrading.api.common.exception.QuoteUnavailableException
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.enums.OrderType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.enums.TriggerType
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.support.withId
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.port.CollectorSubscriptionPort
import com.papertrading.api.domain.port.MarketQuotePort
import com.papertrading.api.application.common.result.QuoteSnapshot
import com.papertrading.api.infrastructure.persistence.AccountLedgerRepository
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.OrderRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional

class OrderCommandServiceTest {

    private val accountRepository = mockk<AccountRepository>()
    private val orderRepository = mockk<OrderRepository>()
    private val positionRepository = mockk<PositionRepository>()
    private val accountLedgerRepository = mockk<AccountLedgerRepository>()
    private val marketQuotePort = mockk<MarketQuotePort>()
    private val collectorSubscriptionPort = mockk<CollectorSubscriptionPort>()
    private val localMatchingEngine = mockk<LocalMatchingEngine>()
    private val kisPaperOrderExecutor = mockk<KisPaperOrderExecutor>()
    private val kisLiveOrderExecutor = mockk<KisLiveOrderExecutor>()

    private val service = OrderCommandService(
        accountRepository, orderRepository, positionRepository, accountLedgerRepository,
        marketQuotePort, collectorSubscriptionPort, localMatchingEngine,
        kisPaperOrderExecutor, kisLiveOrderExecutor,
    )

    private fun localAccount(deposit: BigDecimal = BigDecimal("1000000")): Account = Account.create(
        accountName = "test", accountType = AccountType.STOCK,
        tradingMode = TradingMode.LOCAL, initialDeposit = deposit
    ).withId(1L)

    private fun paperAccount(deposit: BigDecimal = BigDecimal("1000000")): Account = Account.create(
        accountName = "paper", accountType = AccountType.STOCK,
        tradingMode = TradingMode.KIS_PAPER, initialDeposit = deposit
    ).withId(2L)

    private fun liveAccount(deposit: BigDecimal = BigDecimal("1000000")): Account = Account.create(
        accountName = "live", accountType = AccountType.STOCK,
        tradingMode = TradingMode.KIS_LIVE, initialDeposit = deposit
    ).withId(3L)

    private fun savedOrder(account: Account): Order = Order.create(
        account = account,
        ticker = "005930",
        marketType = MarketType.KOSPI,
        orderType = OrderType.LIMIT,
        orderSide = OrderSide.BUY,
        orderCondition = OrderCondition.DAY,
        quantity = BigDecimal("5"),
        limitPrice = BigDecimal("70000"),
        lockedAmount = BigDecimal("350000"),
        idempotencyKey = "key-1",
    ).withId(10L)

    @Test
    fun `지정가 매수 주문 정상 접수`() {
        val account = localAccount()
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { orderRepository.findByAccountIdAndIdempotencyKey(1L, "key-1") } returns null
        every { orderRepository.save(any()) } answers { firstArg<Order>().withId(10L) }
        every { accountLedgerRepository.save(any()) } answers { firstArg() }
        every { collectorSubscriptionPort.subscribe("LOCAL", "005930") } just runs
        every { localMatchingEngine.determineFillPrice(any(), any()) } returns null

        val order = service.placeOrder(1L, PlaceOrderCommand(
            ticker = "005930", marketType = MarketType.KOSPI,
            orderType = OrderType.LIMIT, orderSide = OrderSide.BUY,
            orderCondition = OrderCondition.DAY, quantity = BigDecimal("5"),
            limitPrice = BigDecimal("70000"), expireAt = null, idempotencyKey = "key-1",
        ))

        assertEquals(OrderStatus.PENDING, order.orderStatus)
        assertEquals(BigDecimal("650000"), account.availableDeposit) // 1000000 - 350000
        verify { collectorSubscriptionPort.subscribe("LOCAL", "005930") }
    }

    @Test
    fun `시장가 매수 — stale 시세 시 예외`() {
        val account = localAccount()
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { orderRepository.findByAccountIdAndIdempotencyKey(1L, "key-2") } returns null
        every { marketQuotePort.getQuote(TradingMode.LOCAL, "005930") } returns null

        assertThrows<QuoteUnavailableException> {
            service.placeOrder(1L, PlaceOrderCommand(
                ticker = "005930", marketType = MarketType.KOSPI,
                orderType = OrderType.MARKET, orderSide = OrderSide.BUY,
                orderCondition = OrderCondition.DAY, quantity = BigDecimal("1"),
                limitPrice = null, expireAt = null, idempotencyKey = "key-2",
            ))
        }
    }

    @Test
    fun `미보유 종목 매도 시 예외`() {
        val account = localAccount()
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { orderRepository.findByAccountIdAndIdempotencyKey(1L, "key-3") } returns null
        every { positionRepository.findByAccountIdAndTickerWithLock(1L, "005930") } returns Optional.empty()

        assertThrows<PositionNotFoundException> {
            service.placeOrder(1L, PlaceOrderCommand(
                ticker = "005930", marketType = MarketType.KOSPI,
                orderType = OrderType.LIMIT, orderSide = OrderSide.SELL,
                orderCondition = OrderCondition.DAY, quantity = BigDecimal("3"),
                limitPrice = BigDecimal("75000"), expireAt = null, idempotencyKey = "key-3",
            ))
        }
    }

    @Test
    fun `IOC 주문 — 시세 없으면 즉시 취소`() {
        val account = localAccount()
        val savedOrderObj = savedOrder(account)
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { orderRepository.findByAccountIdAndIdempotencyKey(1L, "key-4") } returns null
        every { orderRepository.save(any()) } returns savedOrderObj
        every { accountLedgerRepository.save(any()) } answers { firstArg() }
        every { collectorSubscriptionPort.subscribe("LOCAL", "005930") } just runs
        every { marketQuotePort.getQuote(TradingMode.LOCAL, "005930") } returns null
        // cancelOrder 호출
        every { orderRepository.findByIdWithOptimisticLock(10L) } returns Optional.of(savedOrderObj)

        service.placeOrder(1L, PlaceOrderCommand(
            ticker = "005930", marketType = MarketType.KOSPI,
            orderType = OrderType.LIMIT, orderSide = OrderSide.BUY,
            orderCondition = OrderCondition.IOC, quantity = BigDecimal("5"),
            limitPrice = BigDecimal("70000"), expireAt = null, idempotencyKey = "key-4",
        ))

        assertEquals(OrderStatus.CANCELLED, savedOrderObj.orderStatus)
    }

    @Test
    fun `시장가 매수 — 정상 시세 시 주문 접수`() {
        val account = localAccount()
        val quote = QuoteSnapshot("005930", TradingMode.LOCAL, BigDecimal("72000"), BigDecimal("72100"), BigDecimal("71900"), Instant.now())
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { orderRepository.findByAccountIdAndIdempotencyKey(1L, "key-5") } returns null
        every { marketQuotePort.getQuote(TradingMode.LOCAL, "005930") } returns quote
        every { orderRepository.save(any()) } answers { firstArg<Order>().withId(11L) }
        every { accountLedgerRepository.save(any()) } answers { firstArg() }
        every { collectorSubscriptionPort.subscribe("LOCAL", "005930") } just runs

        val order = service.placeOrder(1L, PlaceOrderCommand(
            ticker = "005930", marketType = MarketType.KOSPI,
            orderType = OrderType.MARKET, orderSide = OrderSide.BUY,
            orderCondition = OrderCondition.DAY, quantity = BigDecimal("2"),
            limitPrice = null, expireAt = null, idempotencyKey = "key-5",
        ))

        assertEquals(OrderStatus.PENDING, order.orderStatus)
        // 72000 * 2 = 144000 잠금
        assertEquals(BigDecimal("856000"), account.availableDeposit)
    }

    @Test
    fun `KIS_PAPER 주문은 paper executor를 호출한다`() {
        val account = paperAccount()
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { orderRepository.findByAccountIdAndIdempotencyKey(1L, "key-paper") } returns null
        every { orderRepository.save(any()) } answers { firstArg<Order>().withId(21L) }
        every { accountLedgerRepository.save(any()) } answers { firstArg() }
        every { collectorSubscriptionPort.subscribe("KIS_PAPER", "005930") } just runs
        every { kisPaperOrderExecutor.submit(any()) } just runs

        service.placeOrder(1L, PlaceOrderCommand(
            ticker = "005930", marketType = MarketType.KOSPI,
            orderType = OrderType.LIMIT, orderSide = OrderSide.BUY,
            orderCondition = OrderCondition.DAY, quantity = BigDecimal("1"),
            limitPrice = BigDecimal("70000"), expireAt = null, idempotencyKey = "key-paper",
        ))

        verify(exactly = 1) { kisPaperOrderExecutor.submit(any()) }
        verify(exactly = 0) { kisLiveOrderExecutor.submit(any()) }
    }

    @Test
    fun `KIS_LIVE 주문은 live executor를 호출한다`() {
        val account = liveAccount()
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { orderRepository.findByAccountIdAndIdempotencyKey(1L, "key-live") } returns null
        every { orderRepository.save(any()) } answers { firstArg<Order>().withId(31L) }
        every { accountLedgerRepository.save(any()) } answers { firstArg() }
        every { collectorSubscriptionPort.subscribe("KIS_LIVE", "005930") } just runs
        every { kisLiveOrderExecutor.submit(any()) } just runs

        service.placeOrder(1L, PlaceOrderCommand(
            ticker = "005930", marketType = MarketType.KOSPI,
            orderType = OrderType.LIMIT, orderSide = OrderSide.BUY,
            orderCondition = OrderCondition.DAY, quantity = BigDecimal("1"),
            limitPrice = BigDecimal("70000"), expireAt = null, idempotencyKey = "key-live",
        ))

        verify(exactly = 1) { kisLiveOrderExecutor.submit(any()) }
        verify(exactly = 0) { kisPaperOrderExecutor.submit(any()) }
    }

    // ---- 브로커 주문 연동이 없는 모드는 자금 잠금 전에 501 로 거부 (대상 모드는 하단 표에서 파생) ----

    private fun accountFor(tradingMode: TradingMode): Account = Account.create(
        accountName = "mode-$tradingMode",
        accountType = if (tradingMode == TradingMode.UPBIT_LIVE) AccountType.CRYPTO else AccountType.STOCK,
        tradingMode = tradingMode,
        initialDeposit = BigDecimal("1000000"),
    ).withId(1L)

    private fun stubAccountLookup(account: Account, idempotencyKey: String) {
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { orderRepository.findByAccountIdAndIdempotencyKey(1L, idempotencyKey) } returns null
    }

    private fun command(
        orderType: OrderType,
        orderSide: OrderSide,
        orderCondition: OrderCondition = OrderCondition.DAY,
        idempotencyKey: String,
    ) = PlaceOrderCommand(
        ticker = "005930", marketType = MarketType.KOSPI,
        orderType = orderType, orderSide = orderSide,
        orderCondition = orderCondition, quantity = BigDecimal("5"),
        limitPrice = if (orderType == OrderType.LIMIT) BigDecimal("70000") else null,
        expireAt = null, idempotencyKey = idempotencyKey,
    )

    private fun assertRejectedAsBrokerOrderNotSupported(block: () -> Unit) {
        val ex = assertThrows<BrokerOrderNotSupportedException> { block() }
        assertEquals("BROKER_ORDER_NOT_SUPPORTED", ex.errorCode)
        assertEquals(HttpStatus.NOT_IMPLEMENTED, ex.status)
    }

    private fun verifyNoOrderSideEffects() {
        verify(exactly = 0) { orderRepository.save(any()) }
        verify(exactly = 0) { accountLedgerRepository.save(any()) }
        verify(exactly = 0) { positionRepository.findByAccountIdAndTickerWithLock(any(), any()) }
        verify(exactly = 0) { collectorSubscriptionPort.subscribe(any(), any()) }
        verify(exactly = 0) { marketQuotePort.getQuote(any(), any()) }
        verify(exactly = 0) { kisPaperOrderExecutor.submit(any()) }
        verify(exactly = 0) { kisLiveOrderExecutor.submit(any()) }
    }

    private fun assertDepositUntouched(account: Account) {
        assertEquals(0, BigDecimal("1000000").compareTo(account.availableDeposit))
        assertEquals(0, BigDecimal.ZERO.compareTo(account.lockedDeposit))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("brokerOrderUnsupportedModes")
    fun `미지원 모드 지정가 매수 — 501 거부, 저장·원장·잠금·구독 없음`(mode: TradingMode) {
        val account = accountFor(mode)
        stubAccountLookup(account, "limit-buy-$mode")

        assertRejectedAsBrokerOrderNotSupported {
            service.placeOrder(1L, command(OrderType.LIMIT, OrderSide.BUY, idempotencyKey = "limit-buy-$mode"))
        }

        assertDepositUntouched(account)
        verifyNoOrderSideEffects()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("brokerOrderUnsupportedModes")
    fun `미지원 모드 시장가 매수 — 시세 조회 없이 501 거부`(mode: TradingMode) {
        val account = accountFor(mode)
        stubAccountLookup(account, "market-buy-$mode")

        assertRejectedAsBrokerOrderNotSupported {
            service.placeOrder(1L, command(OrderType.MARKET, OrderSide.BUY, idempotencyKey = "market-buy-$mode"))
        }

        assertDepositUntouched(account)
        verifyNoOrderSideEffects()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("brokerOrderUnsupportedModes")
    fun `미지원 모드 매도 — 포지션 수량 잠금 없이 501 거부`(mode: TradingMode) {
        val account = accountFor(mode)
        stubAccountLookup(account, "sell-$mode")

        assertRejectedAsBrokerOrderNotSupported {
            service.placeOrder(1L, command(OrderType.LIMIT, OrderSide.SELL, idempotencyKey = "sell-$mode"))
        }

        verifyNoOrderSideEffects()
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("brokerOrderUnsupportedModesWithIocFok")
    fun `미지원 모드 IOC, FOK — 취소 주문을 남기지 않고 501 거부`(mode: TradingMode, condition: OrderCondition) {
        val account = accountFor(mode)
        val key = "$condition-$mode"
        stubAccountLookup(account, key)

        assertRejectedAsBrokerOrderNotSupported {
            service.placeOrder(1L, command(OrderType.LIMIT, OrderSide.BUY, condition, key))
        }

        assertDepositUntouched(account)
        verifyNoOrderSideEffects()
        verify(exactly = 0) { orderRepository.findByIdWithOptimisticLock(any()) }
    }

    // 조회 0회 단언(verifyNoOrderSideEffects)이 수량 잠금 부재의 증거
    @ParameterizedTest(name = "{0}")
    @MethodSource("brokerOrderUnsupportedModes")
    fun `미지원 모드 자동 청산 매도 — placeOrder 와 같은 거부를 탄다`(mode: TradingMode) {
        val account = accountFor(mode)
        val position = Position.create(account = account, ticker = "005930", marketType = MarketType.KOSPI)
            .apply { applyBuy(BigDecimal("5"), BigDecimal("60000")) }
            .withId(77L)
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { orderRepository.findByAccountIdAndIdempotencyKey(1L, any()) } returns null

        assertRejectedAsBrokerOrderNotSupported {
            service.createAutoExitSellOrder(position, 1L, TriggerType.STOP_LOSS)
        }

        verifyNoOrderSideEffects()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("brokerOrderUnsupportedModes")
    fun `미지원 모드 그룹 자동 청산 매도 — placeOrder 와 같은 거부를 탄다`(mode: TradingMode) {
        val account = accountFor(mode)
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { orderRepository.findByAccountIdAndIdempotencyKey(1L, any()) } returns null

        assertRejectedAsBrokerOrderNotSupported {
            service.createGroupedAutoExitSellOrder(
                accountId = 1L,
                ticker = "005930",
                marketType = MarketType.KOSPI,
                quantity = BigDecimal("5"),
                orderGroupId = "group-$mode",
                triggerAuditInputs = listOf(AutoExitTriggerAuditInput(triggerId = 1L, triggerVersion = 1L)),
            )
        }

        verifyNoOrderSideEffects()
    }

    // ---- 모드 누락 표: 새 TradingMode 를 추가하면 이 표에 주문 동작을 적어야 통과한다 ----

    private enum class OrderRouting { INTERNAL_MATCHING, KIS_PAPER_EXECUTOR, KIS_LIVE_EXECUTOR, REJECTED }

    // 라우팅마다 기대 호출 수를 전수 when 으로 명시 (새 라우팅 값은 컴파일 에러)
    private data class ExpectedCalls(val saved: Int, val paperExecutor: Int, val liveExecutor: Int)

    private fun expectedCalls(routing: OrderRouting): ExpectedCalls = when (routing) {
        OrderRouting.INTERNAL_MATCHING -> ExpectedCalls(saved = 1, paperExecutor = 0, liveExecutor = 0)
        OrderRouting.KIS_PAPER_EXECUTOR -> ExpectedCalls(saved = 1, paperExecutor = 1, liveExecutor = 0)
        OrderRouting.KIS_LIVE_EXECUTOR -> ExpectedCalls(saved = 1, paperExecutor = 0, liveExecutor = 1)
        OrderRouting.REJECTED -> ExpectedCalls(saved = 0, paperExecutor = 0, liveExecutor = 0)
    }

    @Test
    fun `모드 누락 표 — 키 집합이 TradingMode entries 와 같다`() {
        assertEquals(TradingMode.entries.toSet(), EXPECTED_ORDER_ROUTING.keys)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TradingMode::class)
    fun `모드 누락 표 — 모드별 주문 분기`(mode: TradingMode) {
        val routing = EXPECTED_ORDER_ROUTING.getValue(mode)
        val expected = expectedCalls(routing)
        val account = accountFor(mode)
        val key = "table-$mode"
        stubAccountLookup(account, key)
        every { orderRepository.save(any()) } answers { firstArg<Order>().withId(50L) }
        every { accountLedgerRepository.save(any()) } answers { firstArg() }
        every { collectorSubscriptionPort.subscribe(mode.name, "005930") } just runs
        every { kisPaperOrderExecutor.submit(any()) } just runs
        every { kisLiveOrderExecutor.submit(any()) } just runs

        val place = { service.placeOrder(1L, command(OrderType.LIMIT, OrderSide.BUY, idempotencyKey = key)) }
        when (routing) {
            OrderRouting.REJECTED -> assertRejectedAsBrokerOrderNotSupported { place() }
            OrderRouting.INTERNAL_MATCHING, OrderRouting.KIS_PAPER_EXECUTOR, OrderRouting.KIS_LIVE_EXECUTOR -> place()
        }

        verify(exactly = expected.saved) { orderRepository.save(any()) }
        verify(exactly = expected.paperExecutor) { kisPaperOrderExecutor.submit(any()) }
        verify(exactly = expected.liveExecutor) { kisLiveOrderExecutor.submit(any()) }
    }

    companion object {
        private val EXPECTED_ORDER_ROUTING: Map<TradingMode, OrderRouting> = mapOf(
            TradingMode.LOCAL to OrderRouting.INTERNAL_MATCHING,
            TradingMode.KIS_PAPER to OrderRouting.KIS_PAPER_EXECUTOR,
            TradingMode.KIS_LIVE to OrderRouting.KIS_LIVE_EXECUTOR,
            TradingMode.TOSS_LIVE to OrderRouting.REJECTED,
            TradingMode.UPBIT_LIVE to OrderRouting.REJECTED,
        )

        // 거부 모드는 표에서 파생해 한 곳에서만 관리
        @JvmStatic
        fun brokerOrderUnsupportedModes(): List<TradingMode> =
            EXPECTED_ORDER_ROUTING.filterValues { it == OrderRouting.REJECTED }.keys.toList()

        @JvmStatic
        fun brokerOrderUnsupportedModesWithIocFok(): List<Arguments> =
            brokerOrderUnsupportedModes().flatMap { mode ->
                listOf(OrderCondition.IOC, OrderCondition.FOK).map { Arguments.of(mode, it) }
            }
    }
}
