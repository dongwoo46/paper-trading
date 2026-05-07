package com.papertrading.api.application.portfolio

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.portfolio.DailyBalance
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.DailyBalanceRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import com.papertrading.api.support.withId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class DailyBalanceCommandServiceTest {
    private val accountRepository = mockk<AccountRepository>()
    private val positionRepository = mockk<PositionRepository>()
    private val dailyBalanceRepository = mockk<DailyBalanceRepository>()

    private val service = DailyBalanceCommandService(
        accountRepository = accountRepository,
        positionRepository = positionRepository,
        dailyBalanceRepository = dailyBalanceRepository,
    )

    @Test
    fun `recalculate computes and creates daily balance`() {
        val account = account()
        val position = Position.createWithHolding(
            account = account,
            ticker = "005930",
            marketType = MarketType.KOSPI,
            quantity = BigDecimal("10"),
            avgBuyPrice = BigDecimal("65000"),
            currentPrice = BigDecimal("70000"),
        )

        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { positionRepository.findByAccountIdAndQuantityGreaterThan(1L, BigDecimal.ZERO) } returns listOf(position)
        every { dailyBalanceRepository.findByAccountIdAndBusinessDate(1L, LocalDate.of(2026, 5, 6)) } returns Optional.empty()
        every { dailyBalanceRepository.save(any<DailyBalance>()) } answers { firstArg() }

        val result = service.recalculate(1L, LocalDate.of(2026, 5, 6))

        assertEquals(BigDecimal("1000000"), result.cashBalance)
        assertEquals(BigDecimal("700000"), result.stockMarketValue)
        assertEquals(BigDecimal("1700000"), result.totalAssetValue)
        assertEquals(BigDecimal("700000"), result.pnlAmount)
        assertEquals(BigDecimal("0.700000"), result.pnlRate)
    }

    @Test
    fun `recalculate updates existing snapshot for same account and date`() {
        val account = account()
        val businessDate = LocalDate.of(2026, 5, 6)
        val existing = DailyBalance.create(
            account = account,
            businessDate = businessDate,
            cashBalance = BigDecimal("10"),
            stockMarketValue = BigDecimal("20"),
            totalAssetValue = BigDecimal("30"),
            pnlAmount = BigDecimal("40"),
            pnlRate = BigDecimal("0.1"),
        )

        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { positionRepository.findByAccountIdAndQuantityGreaterThan(1L, BigDecimal.ZERO) } returns emptyList()
        every { dailyBalanceRepository.findByAccountIdAndBusinessDate(1L, businessDate) } returns Optional.of(existing)
        every { dailyBalanceRepository.save(any<DailyBalance>()) } answers { firstArg() }

        val result = service.recalculate(1L, businessDate)

        assertEquals(existing, result)
        assertEquals(BigDecimal("1000000"), result.cashBalance)
        assertEquals(BigDecimal.ZERO, result.stockMarketValue)
        verify(exactly = 1) { dailyBalanceRepository.save(existing) }
    }

    private fun account(): Account = Account.create(
        accountName = "test",
        accountType = AccountType.STOCK,
        tradingMode = TradingMode.LOCAL,
        initialDeposit = BigDecimal("1000000"),
    ).withId(1L)
}
