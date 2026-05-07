package com.papertrading.api.domain.entity.order

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderType
import com.papertrading.api.domain.enums.TradingMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class OrderAmendmentTest {

    @Test
    fun modify_정정_이력을_생성한다() {
        val order = createOrder()

        val amendment = OrderAmendment.modify(
            order = order,
            beforeQuantity = BigDecimal("10"),
            afterQuantity = BigDecimal("5"),
            beforeLimitPrice = BigDecimal("70000"),
            afterLimitPrice = BigDecimal("69000"),
            reason = "가격 조정"
        )

        assertEquals("MODIFY", amendment.amendmentType)
        assertEquals(0, amendment.beforeQuantity!!.compareTo(BigDecimal("10")))
        assertEquals(0, amendment.afterQuantity!!.compareTo(BigDecimal("5")))
        assertEquals(0, amendment.beforeLimitPrice!!.compareTo(BigDecimal("70000")))
        assertEquals("가격 조정", amendment.reason)
    }

    @Test
    fun cancel_취소_이력을_생성하면_beforeQuantity가_주문수량과_같다() {
        val order = createOrder(qty = BigDecimal("10"))

        val amendment = OrderAmendment.cancel(order, reason = "투자자 취소 요청")

        assertEquals("CANCEL", amendment.amendmentType)
        assertEquals(0, amendment.beforeQuantity!!.compareTo(BigDecimal("10")))
        assertNull(amendment.afterQuantity)
        assertEquals("투자자 취소 요청", amendment.reason)
    }

    @Test
    fun create_유효하지_않은_amendmentType이면_예외를_던진다() {
        assertThrows(IllegalArgumentException::class.java) {
            OrderAmendment.create(
                order = createOrder(),
                amendmentType = "UNKNOWN",
                reason = "테스트"
            )
        }
    }

    @Test
    fun create_사유_없이도_생성_가능하다() {
        val amendment = OrderAmendment.create(
            order = createOrder(),
            amendmentType = "CANCEL"
        )

        assertNull(amendment.reason)
    }

    @Test
    fun create_선택_필드가_없어도_생성된다() {
        val amendment = OrderAmendment.modify(order = createOrder())

        assertEquals("MODIFY", amendment.amendmentType)
        assertNull(amendment.beforeQuantity)
        assertNull(amendment.afterQuantity)
        assertNull(amendment.beforeLimitPrice)
        assertNull(amendment.afterLimitPrice)
    }

    // --- helpers ---

    private fun createOrder(qty: BigDecimal = BigDecimal("10")): Order = Order.create(
        account = Account.create(
            accountName = "test-account",
            accountType = AccountType.STOCK,
            tradingMode = TradingMode.LOCAL,
            initialDeposit = BigDecimal("10000000")
        ),
        ticker = "005930",
        marketType = MarketType.KOSPI,
        orderType = OrderType.LIMIT,
        orderSide = OrderSide.BUY,
        orderCondition = OrderCondition.GTC,
        quantity = qty,
        limitPrice = BigDecimal("70000"),
        lockedAmount = qty.multiply(BigDecimal("70000")),
        idempotencyKey = "amend-key-${System.nanoTime()}"
    )
}
