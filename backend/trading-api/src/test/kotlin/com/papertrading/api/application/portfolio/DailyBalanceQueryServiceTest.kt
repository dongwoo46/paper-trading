package com.papertrading.api.application.portfolio

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.portfolio.DailyBalance
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.DailyBalanceRepository
import com.papertrading.api.support.withId
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class DailyBalanceQueryServiceTest {
    private val accountRepository = mockk<AccountRepository>()
    private val dailyBalanceRepository = mockk<DailyBalanceRepository>()

    private val service = DailyBalanceQueryService(accountRepository, dailyBalanceRepository)

    @Test
    fun `getDailyBalances returns balances in range`() {
        val account = account()
        val from = LocalDate.of(2026, 5, 1)
        val to = LocalDate.of(2026, 5, 2)
        val balances = listOf(
            dailyBalance(account, LocalDate.of(2026, 5, 1)),
            dailyBalance(account, LocalDate.of(2026, 5, 2)),
        )

        every { accountRepository.findById(1L) } returns Optional.of(account)
        every { dailyBalanceRepository.findByAccountIdAndBusinessDateBetweenOrderByBusinessDateAsc(1L, from, to) } returns balances

        val result = service.getDailyBalances(1L, from, to)

        assertEquals(2, result.size)
        assertEquals(LocalDate.of(2026, 5, 1), result[0].businessDate)
    }

    @Test
    fun `getDailyBalances throws INVALID_DATE_RANGE when fromDate after toDate`() {
        assertThrows<InvalidDateRangeException> {
            service.getDailyBalances(1L, LocalDate.of(2026, 5, 3), LocalDate.of(2026, 5, 1))
        }
    }

    private fun account(): Account = Account.create(
        accountName = "test",
        accountType = AccountType.STOCK,
        tradingMode = TradingMode.LOCAL,
        initialDeposit = BigDecimal("1000000"),
    ).withId(1L)

    private fun dailyBalance(account: Account, date: LocalDate): DailyBalance = DailyBalance.create(
        account = account,
        businessDate = date,
        cashBalance = BigDecimal("1000000"),
        stockMarketValue = BigDecimal("10000"),
        totalAssetValue = BigDecimal("1010000"),
        pnlAmount = BigDecimal("10000"),
        pnlRate = BigDecimal("0.010000"),
    )
}