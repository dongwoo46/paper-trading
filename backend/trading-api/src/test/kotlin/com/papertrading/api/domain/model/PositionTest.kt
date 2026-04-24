package com.papertrading.api.domain.model

import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.PriceSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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

    // --- 추가 도메인 테스트 (TDD Red) ---

    @Test
    fun `position_avg_price_recalculated_after_buy_fill`() {
        // 초기 0주 상태에서 첫 매수
        val p = Position(
            ticker = "005930",
            marketType = MarketType.KOSPI,
            quantity = BigDecimal.ZERO,
            lockedQuantity = BigDecimal.ZERO,
            orderableQuantity = BigDecimal.ZERO,
            avgBuyPrice = BigDecimal.ZERO,
            totalBuyAmount = BigDecimal.ZERO,
        )
        p.applyBuy(BigDecimal("10"), BigDecimal("70000"))
        assertEquals(0, p.avgBuyPrice.compareTo(BigDecimal("70000")))
        assertEquals(0, p.quantity.compareTo(BigDecimal("10")))
        assertEquals(0, p.totalBuyAmount.compareTo(BigDecimal("700000")))
    }

    @Test
    fun `position_total_buy_amount_proportionally_reduced_after_sell_fill`() {
        val p = position(qty = BigDecimal("10"), locked = BigDecimal("5"))
        // totalBuyAmount = 700000, avgBuyPrice = 70000
        p.applySell(BigDecimal("5"))
        // remaining = 5, totalBuyAmount = 70000 * 5 = 350000
        assertEquals(0, p.totalBuyAmount.compareTo(BigDecimal("350000")))
        assertEquals(0, p.quantity.compareTo(BigDecimal("5")))
    }

    @Test
    fun `position_evaluation_amount_and_pnl_calculated_after_update_price`() {
        val p = position(qty = BigDecimal("10"))
        // avgBuyPrice = 70000, totalBuyAmount = 700000
        p.updatePrice(BigDecimal("75000"), PriceSource.REDIS_LIVE)

        // evaluationAmount = 75000 * 10 = 750000
        assertEquals(0, p.evaluationAmount?.compareTo(BigDecimal("750000")))
        // unrealizedPnl = 750000 - 700000 = 50000
        assertEquals(0, p.unrealizedPnl?.compareTo(BigDecimal("50000")))
        // returnRate = (75000 - 70000) / 70000 = 0.0714
        assertEquals(0, p.returnRate?.compareTo(BigDecimal("0.0714")))
        assertEquals(PriceSource.REDIS_LIVE, p.priceSource)
        assertNotNull(p.priceUpdatedAt)
    }

    @Test
    fun `position_return_rate_not_set_when_avg_buy_price_is_zero`() {
        val p = Position(
            ticker = "005930",
            marketType = MarketType.KOSPI,
            quantity = BigDecimal("10"),
            lockedQuantity = BigDecimal.ZERO,
            orderableQuantity = BigDecimal("10"),
            avgBuyPrice = BigDecimal.ZERO,
            totalBuyAmount = BigDecimal.ZERO,
        )
        p.updatePrice(BigDecimal("75000"), PriceSource.REDIS_LIVE)
        // avgBuyPrice == 0이면 returnRate 계산 안 함
        assertNull(p.returnRate)
    }
}
