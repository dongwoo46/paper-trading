package com.papertrading.api.application.position

import com.papertrading.api.application.position.command.CreatePositionExitTriggerCommand
import com.papertrading.api.application.position.command.UpdatePositionExitTriggerCommand
import com.papertrading.api.common.exception.BadRequestException
import com.papertrading.api.common.exception.PositionNotEligibleException
import com.papertrading.api.common.exception.StaleTriggerVersionException
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.entity.position.PositionExitTrigger
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.PriceBasisPolicy
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.enums.TriggerState
import com.papertrading.api.domain.enums.TriggerType
import com.papertrading.api.infrastructure.persistence.PositionExitTriggerRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import com.papertrading.api.support.withId
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Optional

class PositionExitTriggerCommandServiceTest {
    private val positionRepository = mockk<PositionRepository>()
    private val triggerRepository = mockk<PositionExitTriggerRepository>(relaxed = true)
    private val service = PositionExitTriggerCommandService(positionRepository, triggerRepository)

    @Test
    fun `fixed price stop loss trigger is created with default exit ratio`() {
        val position = openPosition(avgBuyPrice = BigDecimal("100.0000"))
        every { positionRepository.findByIdWithLock(10L) } returns Optional.of(position)
        every { triggerRepository.save(any()) } answers { firstArg<PositionExitTrigger>().withId(100L) }

        val result = service.createPositionTrigger(
            CreatePositionExitTriggerCommand(
                positionId = 10L,
                triggerType = TriggerType.STOP_LOSS,
                triggerPercent = null,
                triggerPrice = BigDecimal("95.0000"),
                priceBasisPolicy = PriceBasisPolicy.FIXED_PRICE,
            )
        )

        assertEquals(100L, result.id)
        assertEquals(10L, result.positionId)
        assertEquals(1L, result.accountId)
        assertEquals("005930", result.ticker)
        assertEquals(TriggerType.STOP_LOSS, result.triggerType)
        assertNull(result.triggerPercent)
        assertEquals(BigDecimal("95.0000"), result.triggerPrice)
        assertEquals(PriceBasisPolicy.FIXED_PRICE, result.priceBasisPolicy)
        assertEquals(BigDecimal("100.0000"), result.exitRatioPercent)
        assertEquals(TriggerState.ARMED, result.state)
    }

    @Test
    fun `avg price at creation computes trigger price and keeps requested exit ratio`() {
        val position = openPosition(avgBuyPrice = BigDecimal("100.0000"))
        every { positionRepository.findByIdWithLock(10L) } returns Optional.of(position)
        every { triggerRepository.save(any()) } answers { firstArg<PositionExitTrigger>().withId(101L) }

        val result = service.createPositionTrigger(
            CreatePositionExitTriggerCommand(
                positionId = 10L,
                triggerType = TriggerType.TAKE_PROFIT,
                triggerPercent = BigDecimal("7.0000"),
                triggerPrice = null,
                priceBasisPolicy = PriceBasisPolicy.AVG_PRICE_AT_CREATION,
                exitRatioPercent = BigDecimal("25.5000"),
            )
        )

        assertEquals(TriggerType.TAKE_PROFIT, result.triggerType)
        assertEquals(BigDecimal("7.0000"), result.triggerPercent)
        assertEquals(BigDecimal("107.0000"), result.triggerPrice)
        assertEquals(PriceBasisPolicy.AVG_PRICE_AT_CREATION, result.priceBasisPolicy)
        assertEquals(BigDecimal("25.5000"), result.exitRatioPercent)
    }

    @Test
    fun `follow avg price stores percent without fixed trigger price`() {
        val position = openPosition(avgBuyPrice = BigDecimal("100.0000"))
        every { positionRepository.findByIdWithLock(10L) } returns Optional.of(position)
        every { triggerRepository.save(any()) } answers { firstArg<PositionExitTrigger>().withId(102L) }

        val result = service.createPositionTrigger(
            CreatePositionExitTriggerCommand(
                positionId = 10L,
                triggerType = TriggerType.STOP_LOSS,
                triggerPercent = BigDecimal("5.2500"),
                triggerPrice = null,
                priceBasisPolicy = PriceBasisPolicy.FOLLOW_AVG_PRICE,
                exitRatioPercent = BigDecimal("50.0000"),
            )
        )

        assertEquals(BigDecimal("5.2500"), result.triggerPercent)
        assertNull(result.triggerPrice)
        assertEquals(PriceBasisPolicy.FOLLOW_AVG_PRICE, result.priceBasisPolicy)
        assertEquals(BigDecimal("50.0000"), result.exitRatioPercent)
    }

    @Test
    fun `computed price policies reject caller supplied trigger price`() {
        val position = openPosition(avgBuyPrice = BigDecimal("100.0000"))
        every { positionRepository.findByIdWithLock(10L) } returns Optional.of(position)

        assertThrows(BadRequestException::class.java) {
            service.createPositionTrigger(
                CreatePositionExitTriggerCommand(
                    positionId = 10L,
                    triggerType = TriggerType.TAKE_PROFIT,
                    triggerPercent = BigDecimal("10.0000"),
                    triggerPrice = BigDecimal("111.0000"),
                    priceBasisPolicy = PriceBasisPolicy.AVG_PRICE_AT_CREATION,
                )
            )
        }
    }

    @Test
    fun `stale expected version rejects update`() {
        val position = openPosition(avgBuyPrice = BigDecimal("100.0000"))
        val trigger = fixedTrigger(id = 100L, type = TriggerType.STOP_LOSS, price = BigDecimal("95.0000"))
        every { positionRepository.findByIdWithLock(10L) } returns Optional.of(position)
        every { triggerRepository.findByIdForUpdate(100L) } returns trigger

        assertThrows(StaleTriggerVersionException::class.java) {
            service.updatePositionTrigger(
                UpdatePositionExitTriggerCommand(
                    positionId = 10L,
                    triggerId = 100L,
                    triggerPercent = null,
                    triggerPrice = BigDecimal("94.0000"),
                    priceBasisPolicy = PriceBasisPolicy.FIXED_PRICE,
                    exitRatioPercent = BigDecimal("80.0000"),
                    expectedVersion = 1L,
                )
            )
        }
    }

    @Test
    fun `closed position rejects trigger creation`() {
        val closedPosition = openPosition(quantity = BigDecimal.ZERO, avgBuyPrice = BigDecimal("100.0000"))
        every { positionRepository.findByIdWithLock(10L) } returns Optional.of(closedPosition)

        assertThrows(PositionNotEligibleException::class.java) {
            service.createPositionTrigger(
                CreatePositionExitTriggerCommand(
                    positionId = 10L,
                    triggerType = TriggerType.STOP_LOSS,
                    triggerPercent = null,
                    triggerPrice = BigDecimal("95.0000"),
                    priceBasisPolicy = PriceBasisPolicy.FIXED_PRICE,
                )
            )
        }
    }

    private fun openPosition(
        quantity: BigDecimal = BigDecimal("10.0000"),
        avgBuyPrice: BigDecimal,
    ): Position {
        val account = Account.create("a", AccountType.STOCK, TradingMode.LOCAL, BigDecimal("1000.0000")).withId(1L)
        return Position.createWithHolding(account, "005930", MarketType.KOSPI, quantity, avgBuyPrice).withId(10L)
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
        ).withId(id)
}
