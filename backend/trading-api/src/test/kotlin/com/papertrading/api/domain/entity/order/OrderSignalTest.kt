package com.papertrading.api.domain.entity.order

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.SignalCategory
import com.papertrading.api.domain.enums.SignalIndicator
import com.papertrading.api.domain.enums.TradingMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class OrderSignalTest {

    @Test
    fun create_매수_신호를_생성한다() {
        val signal = createSignal(side = OrderSide.BUY, qty = BigDecimal("10"))

        assertEquals("005930", signal.ticker)
        assertEquals(OrderSide.BUY, signal.signalSide)
        assertEquals(0, signal.quantity.compareTo(BigDecimal("10")))
        assertFalse(signal.isProcessed)
    }

    @Test
    fun create_종목코드가_소문자이면_대문자로_정규화된다() {
        val signal = OrderSignal.create(
            account = createAccount(),
            ticker = "aapl",
            signalSide = OrderSide.BUY,
            quantity = BigDecimal("5"),
            idempotencyKey = "sig-aapl"
        )

        assertEquals("AAPL", signal.ticker)
    }

    @Test
    fun create_수량이_0이하면_예외를_던진다() {
        assertThrows(IllegalArgumentException::class.java) {
            createSignal(qty = BigDecimal.ZERO)
        }
    }

    @Test
    fun create_idempotencyKey가_빈값이면_예외를_던진다() {
        assertThrows(IllegalArgumentException::class.java) {
            OrderSignal.create(
                account = createAccount(),
                ticker = "005930",
                signalSide = OrderSide.BUY,
                quantity = BigDecimal("10"),
                idempotencyKey = "  "
            )
        }
    }

    @Test
    fun create_지정가가_0이하면_예외를_던진다() {
        assertThrows(IllegalArgumentException::class.java) {
            OrderSignal.create(
                account = createAccount(),
                ticker = "005930",
                signalSide = OrderSide.BUY,
                quantity = BigDecimal("10"),
                idempotencyKey = "sig-001",
                limitPrice = BigDecimal.ZERO
            )
        }
    }

    @Test
    fun create_선택_필드를_포함한_신호를_생성한다() {
        val signal = OrderSignal.create(
            account = createAccount(),
            ticker = "005930",
            signalSide = OrderSide.BUY,
            quantity = BigDecimal("10"),
            idempotencyKey = "sig-full-001",
            strategyId = 42L,
            signalCategory = SignalCategory.MA_CROSS,
            signalIndicator = SignalIndicator.MA20,
            signalThreshold = BigDecimal("0.05"),
            signalActualValue = BigDecimal("0.07"),
            conditionSnapshot = """{"ma20":70000,"ma60":68000}""",
            limitPrice = BigDecimal("70000")
        )

        assertEquals(42L, signal.strategyId)
        assertEquals(SignalCategory.MA_CROSS, signal.signalCategory)
        assertEquals(SignalIndicator.MA20, signal.signalIndicator)
        assertEquals(0, signal.signalThreshold!!.compareTo(BigDecimal("0.05")))
    }

    @Test
    fun markAsProcessed_미처리_신호를_처리됨으로_변경한다() {
        val signal = createSignal()

        signal.markAsProcessed()

        assertTrue(signal.isProcessed)
    }

    @Test
    fun markAsProcessed_이미_처리된_신호에_재처리하면_예외를_던진다() {
        val signal = createSignal()
        signal.markAsProcessed()

        assertThrows(IllegalStateException::class.java) {
            signal.markAsProcessed()
        }

        assertTrue(signal.isProcessed)
    }

    // --- helpers ---

    private fun createSignal(
        side: OrderSide = OrderSide.BUY,
        qty: BigDecimal = BigDecimal("10")
    ): OrderSignal = OrderSignal.create(
        account = createAccount(),
        ticker = "005930",
        signalSide = side,
        quantity = qty,
        idempotencyKey = "sig-key-${System.nanoTime()}"
    )

    private fun createAccount(): Account = Account.create(
        accountName = "test-account",
        accountType = AccountType.STOCK,
        tradingMode = TradingMode.LOCAL,
        initialDeposit = BigDecimal("10000000")
    )
}
