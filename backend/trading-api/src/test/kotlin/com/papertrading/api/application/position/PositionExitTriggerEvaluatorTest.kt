package com.papertrading.api.application.position

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.entity.position.PositionExitTrigger
import com.papertrading.api.domain.enums.TriggerState
import com.papertrading.api.domain.enums.TriggerType
import com.papertrading.api.support.withId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class PositionExitTriggerEvaluatorTest {
    private val evaluator = PositionExitTriggerEvaluator()

    @Test
    fun `stop loss와 take profit 조건을 BigDecimal로 평가한다`() {
        val account = Account.create("a", AccountType.STOCK, TradingMode.LOCAL, BigDecimal("1000")).withId(1L)
        val position = Position.createWithHolding(account, "005930", MarketType.KOSPI, BigDecimal("1"), BigDecimal("100"))
        val trigger = PositionExitTrigger.create(10L, 1L, "005930", true, BigDecimal("3.5"), BigDecimal("7.0"))

        val stop = evaluator.evaluate(position, trigger, BigDecimal("96.5000"), Instant.now())
        assertEquals(TriggerType.STOP_LOSS, stop?.type)

        trigger.stopLossState = TriggerState.CANCELED
        val take = evaluator.evaluate(position, trigger, BigDecimal("107.0000"), Instant.now())
        assertEquals(TriggerType.TAKE_PROFIT, take?.type)
    }

    @Test
    fun `threshold와 quote가 동일하면 트리거된다`() {
        val account = Account.create("a", AccountType.STOCK, TradingMode.LOCAL, BigDecimal("1000")).withId(1L)
        val position = Position.createWithHolding(account, "005930", MarketType.KOSPI, BigDecimal("1"), BigDecimal("100"))

        val stopTrigger = PositionExitTrigger.create(10L, 1L, "005930", true, BigDecimal("5.0"), null)
        val stopDecision = evaluator.evaluate(position, stopTrigger, BigDecimal("95.0000"), Instant.now())
        assertEquals(TriggerType.STOP_LOSS, stopDecision?.type)

        val takeTrigger = PositionExitTrigger.create(10L, 1L, "005930", true, null, BigDecimal("7.0"))
        val takeDecision = evaluator.evaluate(position, takeTrigger, BigDecimal("107.0000"), Instant.now())
        assertEquals(TriggerType.TAKE_PROFIT, takeDecision?.type)
    }

    @Test
    fun `threshold 경계를 넘지 않으면 트리거되지 않는다`() {
        val account = Account.create("a", AccountType.STOCK, TradingMode.LOCAL, BigDecimal("1000")).withId(1L)
        val position = Position.createWithHolding(account, "005930", MarketType.KOSPI, BigDecimal("1"), BigDecimal("100"))
        val trigger = PositionExitTrigger.create(10L, 1L, "005930", true, BigDecimal("5.0"), BigDecimal("7.0"))

        assertNull(evaluator.evaluate(position, trigger, BigDecimal("95.0001"), Instant.now()))
        assertNull(evaluator.evaluate(position, trigger, BigDecimal("106.9999"), Instant.now()))
    }
}
