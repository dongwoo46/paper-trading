package com.papertrading.api.domain.entity.order

import com.papertrading.api.domain.entity.account.Account
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

class ExecutionTest {

    // --- create ---

    @Test
    fun create_정상_체결_레코드를_생성한다() {
        val order = createOrder()
        val account = createAccount()
        val now = Instant.now()

        val execution = Execution.create(
            order = order,
            account = account,
            ticker = "005930",
            executedQuantity = BigDecimal("10"),
            executedPrice = BigDecimal("70000"),
            krwExecutedPrice = BigDecimal("70000"),
            externalExecutionId = "EXT-001",
            executedAt = now,
            fee = BigDecimal("350")
        )

        assertEquals("005930", execution.ticker)
        assertEquals(0, execution.executedQuantity.compareTo(BigDecimal("10")))
        assertEquals(0, execution.executedPrice.compareTo(BigDecimal("70000")))
        assertEquals(0, execution.fee.compareTo(BigDecimal("350")))
        assertEquals("KRW", execution.currency)
        assertEquals("EXT-001", execution.externalExecutionId)
    }

    @Test
    fun create_종목코드가_소문자이면_대문자로_정규화된다() {
        val execution = Execution.create(
            order = createOrder(),
            account = createAccount(),
            ticker = "aapl",
            executedQuantity = BigDecimal("5"),
            executedPrice = BigDecimal("200"),
            krwExecutedPrice = BigDecimal("270000"),
            externalExecutionId = "EXT-002",
            executedAt = Instant.now()
        )

        assertEquals("AAPL", execution.ticker)
    }

    @Test
    fun create_수량이_0이하면_예외를_던진다() {
        assertThrows(IllegalArgumentException::class.java) {
            Execution.create(
                order = createOrder(),
                account = createAccount(),
                ticker = "005930",
                executedQuantity = BigDecimal.ZERO,
                executedPrice = BigDecimal("70000"),
                krwExecutedPrice = BigDecimal("70000"),
                externalExecutionId = "EXT-003",
                executedAt = Instant.now()
            )
        }
    }

    @Test
    fun create_체결가가_0이하면_예외를_던진다() {
        assertThrows(IllegalArgumentException::class.java) {
            Execution.create(
                order = createOrder(),
                account = createAccount(),
                ticker = "005930",
                executedQuantity = BigDecimal("10"),
                executedPrice = BigDecimal.ZERO,
                krwExecutedPrice = BigDecimal("70000"),
                externalExecutionId = "EXT-004",
                executedAt = Instant.now()
            )
        }
    }

    @Test
    fun create_수수료가_음수이면_예외를_던진다() {
        assertThrows(IllegalArgumentException::class.java) {
            Execution.create(
                order = createOrder(),
                account = createAccount(),
                ticker = "005930",
                executedQuantity = BigDecimal("10"),
                executedPrice = BigDecimal("70000"),
                krwExecutedPrice = BigDecimal("70000"),
                externalExecutionId = "EXT-005",
                executedAt = Instant.now(),
                fee = BigDecimal("-1")
            )
        }
    }

    @Test
    fun create_외부체결번호가_빈값이면_예외를_던진다() {
        assertThrows(IllegalArgumentException::class.java) {
            Execution.create(
                order = createOrder(),
                account = createAccount(),
                ticker = "005930",
                executedQuantity = BigDecimal("10"),
                executedPrice = BigDecimal("70000"),
                krwExecutedPrice = BigDecimal("70000"),
                externalExecutionId = "  ",
                executedAt = Instant.now()
            )
        }
    }

    // --- totalCost / totalKrwCost ---

    @Test
    fun totalCost_체결금액과_수수료의_합을_반환한다() {
        val execution = Execution.create(
            order = createOrder(),
            account = createAccount(),
            ticker = "005930",
            executedQuantity = BigDecimal("10"),
            executedPrice = BigDecimal("70000"),
            krwExecutedPrice = BigDecimal("70000"),
            externalExecutionId = "EXT-006",
            executedAt = Instant.now(),
            fee = BigDecimal("350")
        )

        // 10 * 70000 + 350 = 700350
        assertEquals(0, execution.totalCost().compareTo(BigDecimal("700350")))
    }

    @Test
    fun totalKrwCost_원화환산_체결금액과_수수료의_합을_반환한다() {
        val execution = Execution.create(
            order = createOrder(),
            account = createAccount(),
            ticker = "AAPL",
            executedQuantity = BigDecimal("5"),
            executedPrice = BigDecimal("200"),
            krwExecutedPrice = BigDecimal("270000"),
            externalExecutionId = "EXT-007",
            executedAt = Instant.now(),
            fee = BigDecimal("500"),
            currency = "USD",
            fxRate = BigDecimal("1350.0")
        )

        // 5 * 270000 + 500 = 1350500
        assertEquals(0, execution.totalKrwCost().compareTo(BigDecimal("1350500")))
    }

    @Test
    fun create_수수료_기본값은_0이다() {
        val execution = Execution.create(
            order = createOrder(),
            account = createAccount(),
            ticker = "005930",
            executedQuantity = BigDecimal("10"),
            executedPrice = BigDecimal("70000"),
            krwExecutedPrice = BigDecimal("70000"),
            externalExecutionId = "EXT-008",
            executedAt = Instant.now()
        )

        assertEquals(0, execution.fee.compareTo(BigDecimal.ZERO))
        assertEquals(0, execution.totalCost().compareTo(BigDecimal("700000")))
    }

    // --- helpers ---

    private fun createAccount(): Account = Account.create(
        accountName = "test-account",
        accountType = AccountType.STOCK,
        tradingMode = TradingMode.LOCAL,
        initialDeposit = BigDecimal("10000000")
    )

    private fun createOrder(): Order = Order.create(
        account = createAccount(),
        ticker = "005930",
        marketType = MarketType.KOSPI,
        orderType = OrderType.LIMIT,
        orderSide = OrderSide.BUY,
        orderCondition = OrderCondition.GTC,
        quantity = BigDecimal("10"),
        limitPrice = BigDecimal("70000"),
        lockedAmount = BigDecimal("700000"),
        idempotencyKey = "order-key-${System.nanoTime()}"
    )
}
