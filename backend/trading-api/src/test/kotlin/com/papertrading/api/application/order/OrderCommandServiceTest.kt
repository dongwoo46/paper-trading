package com.papertrading.api.application.order

import com.papertrading.api.application.order.command.PlaceOrderCommand
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.enums.OrderType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.model.Account
import com.papertrading.api.domain.model.Order
import com.papertrading.api.domain.model.Position
import com.papertrading.api.domain.port.CollectorSubscriptionPort
import com.papertrading.api.domain.port.MarketQuotePort
import com.papertrading.api.domain.port.QuoteSnapshot
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
    ).apply { id = 1L }

    private fun savedOrder(account: Account): Order = Order(
        id = 10L, account = account, ticker = "005930",
        marketType = MarketType.KOSPI, orderType = OrderType.LIMIT,
        orderSide = OrderSide.BUY, orderCondition = OrderCondition.DAY,
        quantity = BigDecimal("5"), limitPrice = BigDecimal("70000"),
        lockedAmount = BigDecimal("350000"), idempotencyKey = "key-1",
    )

    @Test
    fun `지정가 매수 주문 정상 접수`() {
        val account = localAccount()
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { orderRepository.findByAccountIdAndIdempotencyKey(1L, "key-1") } returns null
        every { orderRepository.save(any()) } answers { firstArg<Order>().apply { id = 10L } }
        every { accountLedgerRepository.save(any()) } answers { firstArg() }
        every { collectorSubscriptionPort.subscribe("live", "005930") } just runs
        every { localMatchingEngine.determineFillPrice(any(), any()) } returns null

        val order = service.placeOrder(1L, PlaceOrderCommand(
            ticker = "005930", marketType = MarketType.KOSPI,
            orderType = OrderType.LIMIT, orderSide = OrderSide.BUY,
            orderCondition = OrderCondition.DAY, quantity = BigDecimal("5"),
            limitPrice = BigDecimal("70000"), expireAt = null, idempotencyKey = "key-1",
        ))

        assertEquals(OrderStatus.PENDING, order.orderStatus)
        assertEquals(BigDecimal("650000"), account.availableDeposit) // 1000000 - 350000
        verify { collectorSubscriptionPort.subscribe("live", "005930") }
    }

    @Test
    fun `시장가 매수 — stale 시세 시 예외`() {
        val account = localAccount()
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { orderRepository.findByAccountIdAndIdempotencyKey(1L, "key-2") } returns null
        every { marketQuotePort.getQuote("005930") } returns null

        assertThrows<IllegalStateException> {
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

        assertThrows<IllegalStateException> {
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
        every { collectorSubscriptionPort.subscribe("live", "005930") } just runs
        every { marketQuotePort.getQuote("005930") } returns null
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
        val quote = QuoteSnapshot("005930", BigDecimal("72000"), BigDecimal("72100"), BigDecimal("71900"), Instant.now())
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { orderRepository.findByAccountIdAndIdempotencyKey(1L, "key-5") } returns null
        every { marketQuotePort.getQuote("005930") } returns quote
        every { orderRepository.save(any()) } answers { firstArg<Order>().apply { id = 11L } }
        every { accountLedgerRepository.save(any()) } answers { firstArg() }
        every { collectorSubscriptionPort.subscribe("live", "005930") } just runs

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
}
