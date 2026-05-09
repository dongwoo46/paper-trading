package com.papertrading.api.application.position

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.entity.position.PositionExitTrigger
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.infrastructure.persistence.PositionExitTriggerRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import com.papertrading.api.support.withId
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Optional

class PositionExitTriggerCommandServiceTest {
    private val positionRepository = mockk<PositionRepository>()
    private val triggerRepository = mockk<PositionExitTriggerRepository>(relaxed = true)
    private val service = PositionExitTriggerCommandService(positionRepository, triggerRepository)

    @Test
    fun `enabled 상태에서 퍼센트 없으면 예외`() {
        val account = Account.create("a", AccountType.STOCK, TradingMode.LOCAL, BigDecimal("1000")).withId(1L)
        val position = Position.createWithHolding(account, "005930", MarketType.KOSPI, BigDecimal("1"), BigDecimal("100")).withId(10L)
        every { positionRepository.findById(10L) } returns Optional.of(position)
        every { triggerRepository.findByPositionId(10L) } returns null
        every { triggerRepository.save(any()) } answers { firstArg<PositionExitTrigger>() }

        assertThrows(IllegalArgumentException::class.java) {
            service.upsertPositionTrigger(UpsertPositionExitTriggerCommand(10L, true, null, null))
        }
    }

    @Test
    fun `퍼센트 경계값 0과 100은 거부된다`() {
        val account = Account.create("a", AccountType.STOCK, TradingMode.LOCAL, BigDecimal("1000")).withId(1L)
        val position = Position.createWithHolding(account, "005930", MarketType.KOSPI, BigDecimal("1"), BigDecimal("100")).withId(10L)
        every { positionRepository.findById(10L) } returns Optional.of(position)
        every { triggerRepository.findByPositionId(10L) } returns null
        every { triggerRepository.save(any()) } answers { firstArg<PositionExitTrigger>() }

        assertThrows(IllegalArgumentException::class.java) {
            service.upsertPositionTrigger(UpsertPositionExitTriggerCommand(10L, true, BigDecimal("0"), BigDecimal("5")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            service.upsertPositionTrigger(UpsertPositionExitTriggerCommand(10L, true, BigDecimal("5"), BigDecimal("100")))
        }
    }

    @Test
    fun `closed position에서는 트리거 설정이 거부되어야 한다`() {
        val account = Account.create("a", AccountType.STOCK, TradingMode.LOCAL, BigDecimal("1000")).withId(1L)
        val closedPosition = Position.createWithHolding(account, "005930", MarketType.KOSPI, BigDecimal.ZERO, BigDecimal("100")).withId(10L)
        every { positionRepository.findById(10L) } returns Optional.of(closedPosition)
        every { triggerRepository.findByPositionId(10L) } returns null

        assertThrows(PositionNotEligibleException::class.java) {
            service.upsertPositionTrigger(UpsertPositionExitTriggerCommand(10L, true, BigDecimal("2.0"), BigDecimal("6.0")))
        }
    }
}
