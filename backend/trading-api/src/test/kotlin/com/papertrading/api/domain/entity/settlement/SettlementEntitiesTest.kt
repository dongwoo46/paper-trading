package com.papertrading.api.domain.entity.settlement

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.order.Execution
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderType
import com.papertrading.api.domain.enums.TradingMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class SettlementEntitiesTest {

    private fun account(): Account = Account.create(
        accountName = "test",
        accountType = AccountType.STOCK,
        tradingMode = TradingMode.LOCAL,
        initialDeposit = BigDecimal("1000000"),
    )

    private fun order(account: Account): Order = Order.create(
        account = account,
        ticker = "005930",
        marketType = MarketType.KOSPI,
        orderType = OrderType.LIMIT,
        orderSide = OrderSide.SELL,
        orderCondition = OrderCondition.DAY,
        quantity = BigDecimal("1"),
        limitPrice = BigDecimal("70000"),
        idempotencyKey = "order-1",
    )

    private fun execution(
        order: Order,
        account: Account,
        ticker: String = "005930",
        qty: BigDecimal = BigDecimal("2"),
        price: BigDecimal = BigDecimal("70000"),
        fee: BigDecimal = BigDecimal("10.0000"),
        currency: String = "KRW",
        fxRate: BigDecimal? = null,
    ): Execution = Execution.create(
        order = order,
        account = account,
        ticker = ticker,
        executedQuantity = qty,
        executedPrice = price,
        fee = fee,
        currency = currency,
        fxRate = fxRate,
        krwExecutedPrice = price,
        externalExecutionId = "exec-${System.nanoTime()}",
        executedAt = Instant.parse("2026-05-08T00:00:00Z"),
    )

    @Test
    fun `pending settlement는 settlementDate 이전에는 완료할 수 없다`() {
        val ps = ReceivableSettlement.create(
            account = account(),
            orderId = 10L,
            settlementDate = LocalDate.of(2026, 5, 12),
            amount = BigDecimal("10000"),
        )

        assertThrows(IllegalStateException::class.java) {
            ps.complete(LocalDate.of(2026, 5, 11))
        }
    }

    @Test
    fun `pending settlement는 settlementDate 당일 이후 완료할 수 있다`() {
        val ps = ReceivableSettlement.create(
            account = account(),
            orderId = 11L,
            settlementDate = LocalDate.of(2026, 5, 12),
            amount = BigDecimal("10000"),
        )

        ps.complete(LocalDate.of(2026, 5, 12))

        assertEquals(com.papertrading.api.domain.enums.SettlementStatus.COMPLETED, ps.status)
    }

    @Test
    fun `외화 settlement는 fxRateAtSettlement가 필요하다`() {
        val acc = account()
        val ord = order(acc)
        val exec = execution(
            order = ord,
            account = acc,
            ticker = "AAPL",
            price = BigDecimal("100.0000"),
            fee = BigDecimal("1.0000"),
            currency = "USD",
            fxRate = null,
        )

        assertThrows(IllegalArgumentException::class.java) {
            Settlement.createFromExecution(
                order = ord,
                account = acc,
                execution = exec,
                tax = BigDecimal("1.0000"),
                settledAt = Instant.parse("2026-05-08T00:00:00Z"),
            )
        }
    }

    @Test
    fun `createFromExecution은 KRW 정산값을 내부 규칙으로 계산한다`() {
        val acc = account()
        val ord = order(acc)
        val exec = execution(order = ord, account = acc, qty = BigDecimal("2"), price = BigDecimal("70000"), fee = BigDecimal("10.0000"))

        val settlement = Settlement.createFromExecution(
            order = ord,
            account = acc,
            execution = exec,
            tax = BigDecimal("5.0000"),
            settledAt = Instant.parse("2026-05-08T01:00:00Z"),
        )

        assertEquals(0, BigDecimal("140000.0000").compareTo(settlement.realizedPnl))
        assertEquals(0, BigDecimal("139985.0000").compareTo(settlement.netPnl))
        assertEquals(0, BigDecimal("139985.0000").compareTo(settlement.krwNetPnl))
    }

    @Test
    fun `createFromExecution은 외화일 때 fxRate를 이용해 krwNetPnl을 계산한다`() {
        val acc = account()
        val ord = order(acc)
        val exec = execution(
            order = ord,
            account = acc,
            ticker = "AAPL",
            qty = BigDecimal("2"),
            price = BigDecimal("100.0000"),
            fee = BigDecimal("1.0000"),
            currency = "USD",
            fxRate = BigDecimal("1300.0000"),
        )

        val settlement = Settlement.createFromExecution(
            order = ord,
            account = acc,
            execution = exec,
            tax = BigDecimal("1.0000"),
            settledAt = Instant.parse("2026-05-08T01:00:00Z"),
        )

        assertEquals(0, BigDecimal("198.0000").compareTo(settlement.netPnl))
        assertEquals(0, BigDecimal("257400.0000").compareTo(settlement.krwNetPnl))
    }
}

