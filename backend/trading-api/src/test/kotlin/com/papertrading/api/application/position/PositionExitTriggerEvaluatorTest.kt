package com.papertrading.api.application.position

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.entity.position.PositionExitTrigger
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.PriceBasisPolicy
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.enums.TriggerType
import com.papertrading.api.support.withId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class PositionExitTriggerEvaluatorTest {
    private val evaluator = PositionExitTriggerEvaluator()
    private val quoteAt = Instant.parse("2026-05-08T12:00:00Z")

    @Test
    fun `stop loss fires when quote is equal to or below trigger price`() {
        val position = position(avgBuyPrice = BigDecimal("100.0000"))
        val trigger = fixedTrigger(100L, TriggerType.STOP_LOSS, BigDecimal("95.0000"))

        val equality = evaluator.evaluate(position, trigger, BigDecimal("95.0000"), quoteAt)
        val below = evaluator.evaluate(position, trigger, BigDecimal("94.9999"), quoteAt)

        assertEquals(TriggerType.STOP_LOSS, equality?.triggerType)
        assertEquals(0, BigDecimal("95.0000").compareTo(equality?.effectiveTriggerPrice))
        assertEquals(TriggerType.STOP_LOSS, below?.triggerType)
    }

    @Test
    fun `stop loss does not fire when quote is above trigger price`() {
        val position = position(avgBuyPrice = BigDecimal("100.0000"))
        val trigger = fixedTrigger(100L, TriggerType.STOP_LOSS, BigDecimal("95.0000"))

        assertNull(evaluator.evaluate(position, trigger, BigDecimal("95.0001"), quoteAt))
    }

    @Test
    fun `take profit fires when quote is equal to or above trigger price`() {
        val position = position(avgBuyPrice = BigDecimal("100.0000"))
        val trigger = fixedTrigger(101L, TriggerType.TAKE_PROFIT, BigDecimal("107.0000"))

        val equality = evaluator.evaluate(position, trigger, BigDecimal("107.0000"), quoteAt)
        val above = evaluator.evaluate(position, trigger, BigDecimal("107.0001"), quoteAt)

        assertEquals(TriggerType.TAKE_PROFIT, equality?.triggerType)
        assertEquals(0, BigDecimal("107.0000").compareTo(equality?.effectiveTriggerPrice))
        assertEquals(TriggerType.TAKE_PROFIT, above?.triggerType)
    }

    @Test
    fun `take profit does not fire when quote is below trigger price`() {
        val position = position(avgBuyPrice = BigDecimal("100.0000"))
        val trigger = fixedTrigger(101L, TriggerType.TAKE_PROFIT, BigDecimal("107.0000"))

        assertNull(evaluator.evaluate(position, trigger, BigDecimal("106.9999"), quoteAt))
    }

    @Test
    fun `follow avg price recomputes threshold from current position average price`() {
        val position = position(avgBuyPrice = BigDecimal("120.0000"))
        val trigger = PositionExitTrigger.create(
            positionId = 10L,
            accountId = 1L,
            ticker = "005930",
            triggerType = TriggerType.STOP_LOSS,
            triggerPercent = BigDecimal("10.0000"),
            triggerPrice = null,
            priceBasisPolicy = PriceBasisPolicy.FOLLOW_AVG_PRICE,
            exitRatioPercent = BigDecimal("40.0000"),
        ).withId(102L)

        val decision = evaluator.evaluate(position, trigger, BigDecimal("108.0000"), quoteAt)

        assertEquals(102L, decision?.triggerId)
        assertEquals(0, BigDecimal("108.00000000").compareTo(decision?.effectiveTriggerPrice))
        assertEquals(BigDecimal("40.0000"), decision?.exitRatioPercent)
    }

    private fun position(avgBuyPrice: BigDecimal): Position {
        val account = Account.create("a", AccountType.STOCK, TradingMode.LOCAL, BigDecimal("1000.0000")).withId(1L)
        return Position.createWithHolding(
            account = account,
            ticker = "005930",
            marketType = MarketType.KOSPI,
            quantity = BigDecimal("10.0000"),
            avgBuyPrice = avgBuyPrice,
        ).withId(10L)
    }

    private fun fixedTrigger(id: Long, type: TriggerType, price: BigDecimal): PositionExitTrigger =
        PositionExitTrigger.create(
            positionId = 10L,
            accountId = 1L,
            ticker = "005930",
            triggerType = type,
            triggerPercent = null,
            triggerPrice = price,
            priceBasisPolicy = PriceBasisPolicy.FIXED_PRICE,
            exitRatioPercent = BigDecimal("100.0000"),
        ).withId(id)
}
