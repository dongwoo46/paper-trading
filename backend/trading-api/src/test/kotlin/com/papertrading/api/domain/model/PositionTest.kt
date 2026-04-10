package com.papertrading.api.domain.model

import com.papertrading.api.domain.enums.MarketType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class PositionTest {

    private fun position(qty: BigDecimal = BigDecimal("10"), locked: BigDecimal = BigDecimal.ZERO) = Position(
        ticker = "005930",
        marketType = MarketType.KOSPI,
        quantity = qty,
        lockedQuantity = locked,
        orderableQuantity = qty.subtract(locked),
        avgBuyPrice = BigDecimal("70000"),
        totalBuyAmount = BigDecimal("70000").multiply(qty),
    )

    @Test
    fun `lockQuantity 정상 잠금`() {
        val p = position(qty = BigDecimal("10"))
        p.lockQuantity(BigDecimal("3"))
        assertEquals(BigDecimal("3"), p.lockedQuantity)
        assertEquals(BigDecimal("7"), p.orderableQuantity)
    }

    @Test
    fun `lockQuantity 수량 초과 시 예외`() {
        val p = position(qty = BigDecimal("10"))
        assertThrows<IllegalArgumentException> { p.lockQuantity(BigDecimal("11")) }
    }

    @Test
    fun `lockQuantity 0 이하 시 예외`() {
        val p = position(qty = BigDecimal("10"))
        assertThrows<IllegalArgumentException> { p.lockQuantity(BigDecimal.ZERO) }
        assertThrows<IllegalArgumentException> { p.lockQuantity(BigDecimal("-1")) }
    }

    @Test
    fun `unlockQuantity 정상 해제`() {
        val p = position(qty = BigDecimal("10"), locked = BigDecimal("5"))
        p.unlockQuantity(BigDecimal("3"))
        assertEquals(BigDecimal("2"), p.lockedQuantity)
        assertEquals(BigDecimal("8"), p.orderableQuantity)
    }

    @Test
    fun `unlockQuantity 잠금 초과 시 예외`() {
        val p = position(qty = BigDecimal("10"), locked = BigDecimal("2"))
        assertThrows<IllegalArgumentException> { p.unlockQuantity(BigDecimal("3")) }
    }

    @Test
    fun `applyBuy 후 평균가와 수량 갱신`() {
        val p = position(qty = BigDecimal("10"))
        p.applyBuy(BigDecimal("5"), BigDecimal("80000"))
        assertEquals(BigDecimal("15"), p.quantity)
        // (70000*10 + 80000*5) / 15 = 1100000/15 = 73333.3333
        assertEquals(0, p.avgBuyPrice.compareTo(BigDecimal("73333.3333")))
    }

    @Test
    fun `applySell 후 수량 감소`() {
        val p = position(qty = BigDecimal("10"), locked = BigDecimal("3"))
        p.applySell(BigDecimal("3"))
        assertEquals(BigDecimal("7"), p.quantity)
        assertEquals(BigDecimal("0"), p.lockedQuantity)
        assertEquals(BigDecimal("7"), p.orderableQuantity)
    }
}
