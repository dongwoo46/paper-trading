package com.papertrading.api.domain.entity.order

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.enums.OrderType
import com.papertrading.api.domain.enums.TradingMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class OrderTest {

    // --- create ---

    @Test
    fun create_지정가_매수_주문을_생성하면_초기_상태는_PENDING이다() {
        val order = createLimitBuyOrder(qty = BigDecimal("10"), price = BigDecimal("70000"))

        assertEquals(OrderStatus.PENDING, order.orderStatus)
        assertEquals("005930", order.ticker)
        assertZero(order.filledQuantity)
        assertZero(order.fee)
        assertNull(order.externalOrderId)
        assertNull(order.avgFilledPrice)
        assertNull(order.totalAmount)
    }

    @Test
    fun create_종목코드가_소문자이면_대문자로_정규화된다() {
        val order = Order.create(
            account = createAccount(),
            ticker = "aapl",
            marketType = MarketType.NASDAQ,
            orderType = OrderType.LIMIT,
            orderSide = OrderSide.BUY,
            orderCondition = OrderCondition.GTC,
            quantity = BigDecimal("5"),
            limitPrice = BigDecimal("200.0000"),
            idempotencyKey = "key-aapl"
        )

        assertEquals("AAPL", order.ticker)
    }

    @Test
    fun create_수량이_0이하면_예외를_던진다() {
        assertThrows(IllegalArgumentException::class.java) {
            Order.create(
                account = createAccount(),
                ticker = "005930",
                marketType = MarketType.KOSPI,
                orderType = OrderType.MARKET,
                orderSide = OrderSide.BUY,
                orderCondition = OrderCondition.GTC,
                quantity = BigDecimal.ZERO,
                idempotencyKey = "key-invalid"
            )
        }
    }

    @Test
    fun create_지정가_주문에_가격이_없으면_예외를_던진다() {
        assertThrows(IllegalArgumentException::class.java) {
            Order.create(
                account = createAccount(),
                ticker = "005930",
                marketType = MarketType.KOSPI,
                orderType = OrderType.LIMIT,
                orderSide = OrderSide.BUY,
                orderCondition = OrderCondition.GTC,
                quantity = BigDecimal("10"),
                limitPrice = null,
                idempotencyKey = "key-no-price"
            )
        }
    }

    @Test
    fun create_idempotencyKey가_빈값이면_예외를_던진다() {
        assertThrows(IllegalArgumentException::class.java) {
            Order.create(
                account = createAccount(),
                ticker = "005930",
                marketType = MarketType.KOSPI,
                orderType = OrderType.MARKET,
                orderSide = OrderSide.BUY,
                orderCondition = OrderCondition.GTC,
                quantity = BigDecimal("10"),
                idempotencyKey = "  "
            )
        }
    }

    // --- applyExecution ---

    @Test
    fun applyExecution_단일_전량_체결시_FILLED로_전환된다() {
        val order = createLimitBuyOrder(qty = BigDecimal("10"), price = BigDecimal("70000"))

        order.applyExecution(
            executedQty = BigDecimal("10"),
            newAvgPrice = BigDecimal("70000"),
            executedFee = BigDecimal("350")
        )

        assertEquals(OrderStatus.FILLED, order.orderStatus)
        assertEquals(BigDecimal("10"), order.filledQuantity)
        assertEquals(BigDecimal("70000"), order.avgFilledPrice)
        assertEquals(0, order.fee.compareTo(BigDecimal("350")))
    }

    @Test
    fun applyExecution_부분_체결시_PARTIAL로_전환된다() {
        val order = createLimitBuyOrder(qty = BigDecimal("10"), price = BigDecimal("70000"))

        order.applyExecution(
            executedQty = BigDecimal("4"),
            newAvgPrice = BigDecimal("70000"),
            executedFee = BigDecimal("140")
        )

        assertEquals(OrderStatus.PARTIAL, order.orderStatus)
        assertEquals(BigDecimal("4"), order.filledQuantity)
    }

    @Test
    fun applyExecution_부분체결_후_잔량_체결되면_FILLED로_전환된다() {
        val order = createLimitBuyOrder(qty = BigDecimal("10"), price = BigDecimal("70000"))
        order.applyExecution(BigDecimal("4"), BigDecimal("70000"), BigDecimal("140"))

        order.applyExecution(BigDecimal("6"), BigDecimal("70000"), BigDecimal("210"))

        assertEquals(OrderStatus.FILLED, order.orderStatus)
        assertEquals(BigDecimal("10"), order.filledQuantity)
    }

    @Test
    fun applyExecution_totalAmount는_누적_체결금액이다() {
        val order = createLimitBuyOrder(qty = BigDecimal("10"), price = BigDecimal("70000"))
        order.applyExecution(BigDecimal("4"), BigDecimal("69000"), BigDecimal("140"))

        order.applyExecution(BigDecimal("6"), BigDecimal("70000"), BigDecimal("210"))

        // totalAmount = 4*69000 + 6*70000 = 276000 + 420000 = 696000
        assertEquals(0, order.totalAmount!!.compareTo(BigDecimal("696000")))
    }

    @Test
    fun applyExecution_CANCELLED_상태에서는_예외를_던진다() {
        val order = createLimitBuyOrder(qty = BigDecimal("10"), price = BigDecimal("70000"))
        order.cancel()

        assertThrows(IllegalStateException::class.java) {
            order.applyExecution(BigDecimal("5"), BigDecimal("70000"), BigDecimal("175"))
        }
    }

    @Test
    fun applyExecution_FILLED_상태에서는_예외를_던진다() {
        val order = createLimitBuyOrder(qty = BigDecimal("5"), price = BigDecimal("70000"))
        order.applyExecution(BigDecimal("5"), BigDecimal("70000"), BigDecimal("175"))

        assertThrows(IllegalStateException::class.java) {
            order.applyExecution(BigDecimal("1"), BigDecimal("70000"), BigDecimal("35"))
        }
    }

    // --- cancel ---

    @Test
    fun cancel_PENDING_주문을_취소하면_CANCELLED로_전환된다() {
        val order = createLimitBuyOrder()

        order.cancel()

        assertEquals(OrderStatus.CANCELLED, order.orderStatus)
    }

    @Test
    fun cancel_PARTIAL_주문을_취소하면_CANCELLED로_전환된다() {
        val order = createLimitBuyOrder(qty = BigDecimal("10"), price = BigDecimal("70000"))
        order.applyExecution(BigDecimal("3"), BigDecimal("70000"), BigDecimal("105"))

        order.cancel()

        assertEquals(OrderStatus.CANCELLED, order.orderStatus)
    }

    @Test
    fun cancel_FILLED_주문은_취소_불가하다() {
        val order = createLimitBuyOrder(qty = BigDecimal("5"), price = BigDecimal("70000"))
        order.applyExecution(BigDecimal("5"), BigDecimal("70000"), BigDecimal("175"))

        assertThrows(IllegalStateException::class.java) {
            order.cancel()
        }
    }

    @Test
    fun cancel_이미_취소된_주문은_재취소_불가하다() {
        val order = createLimitBuyOrder()
        order.cancel()

        assertThrows(IllegalStateException::class.java) {
            order.cancel()
        }
    }

    // --- isCancellable ---

    @Test
    fun isCancellable_PENDING과_PARTIAL만_true를_반환한다() {
        val order = createLimitBuyOrder(qty = BigDecimal("10"), price = BigDecimal("70000"))

        assertTrue(order.isCancellable())

        order.applyExecution(BigDecimal("3"), BigDecimal("70000"), BigDecimal("105"))
        assertTrue(order.isCancellable())

        order.cancel()
        assertFalse(order.isCancellable())
    }

    // --- assignExternalOrderId ---

    @Test
    fun assignExternalOrderId_처음_설정하면_저장된다() {
        val order = createLimitBuyOrder()

        order.assignExternalOrderId("KIS-ORD-12345")

        assertEquals("KIS-ORD-12345", order.externalOrderId)
    }

    @Test
    fun assignExternalOrderId_이미_설정된_경우_예외를_던진다() {
        val order = createLimitBuyOrder()
        order.assignExternalOrderId("KIS-ORD-12345")

        assertThrows(IllegalStateException::class.java) {
            order.assignExternalOrderId("KIS-ORD-99999")
        }

        assertEquals("KIS-ORD-12345", order.externalOrderId)
    }

    // --- remainingLockedAmount ---

    @Test
    fun remainingLockedAmount_체결이_없으면_전액이_잔여잠금이다() {
        val order = createLimitBuyOrder(
            qty = BigDecimal("10"),
            price = BigDecimal("70000"),
            lockedAmount = BigDecimal("700000")
        )

        assertEquals(0, order.remainingLockedAmount().compareTo(BigDecimal("700000")))
    }

    @Test
    fun remainingLockedAmount_체결가가_지정가보다_낮으면_차액이_남는다() {
        val order = createLimitBuyOrder(
            qty = BigDecimal("10"),
            price = BigDecimal("70000"),
            lockedAmount = BigDecimal("700000")
        )
        // 실제 체결가 68000 * 10 = 680000 → 잔여 = 700000 - 680000 = 20000
        order.applyExecution(BigDecimal("10"), BigDecimal("68000"), BigDecimal("340"))

        assertEquals(0, order.remainingLockedAmount().compareTo(BigDecimal("20000")))
    }

    @Test
    fun remainingLockedAmount_매도주문은_잠금이_없어_0이다() {
        val order = Order.create(
            account = createAccount(),
            ticker = "005930",
            marketType = MarketType.KOSPI,
            orderType = OrderType.LIMIT,
            orderSide = OrderSide.SELL,
            orderCondition = OrderCondition.GTC,
            quantity = BigDecimal("10"),
            limitPrice = BigDecimal("70000"),
            lockedAmount = BigDecimal.ZERO,
            idempotencyKey = "sell-key-001"
        )

        assertEquals(0, order.remainingLockedAmount().compareTo(BigDecimal.ZERO))
    }

    // --- helpers ---

    private fun createLimitBuyOrder(
        qty: BigDecimal = BigDecimal("10"),
        price: BigDecimal = BigDecimal("70000"),
        lockedAmount: BigDecimal = BigDecimal("700000")
    ): Order = Order.create(
        account = createAccount(),
        ticker = "005930",
        marketType = MarketType.KOSPI,
        orderType = OrderType.LIMIT,
        orderSide = OrderSide.BUY,
        orderCondition = OrderCondition.GTC,
        quantity = qty,
        limitPrice = price,
        lockedAmount = lockedAmount,
        idempotencyKey = "test-key-${System.nanoTime()}"
    )

    private fun createAccount(): Account = Account.create(
        accountName = "test-account",
        accountType = AccountType.STOCK,
        tradingMode = TradingMode.LOCAL,
        initialDeposit = BigDecimal("10000000")
    )

    private fun assertZero(value: BigDecimal) =
        assertEquals(0, value.compareTo(BigDecimal.ZERO), "Expected zero but was $value")
}
