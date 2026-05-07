package com.papertrading.api.application.portfolio

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.portfolio.PortfolioSnapshot
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.PortfolioSnapshotRepository
import com.papertrading.api.support.withId
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class PortfolioSnapshotQueryServiceTest {
    private val accountRepository = mockk<AccountRepository>()
    private val portfolioSnapshotRepository = mockk<PortfolioSnapshotRepository>()

    private val service = PortfolioSnapshotQueryService(accountRepository, portfolioSnapshotRepository)

    @Test
    fun `getPortfolioSnapshots returns snapshots by date`() {
        val account = account()
        val date = LocalDate.of(2026, 5, 1)
        val snapshots = listOf(
            snapshot(account, date, "000660"),
            snapshot(account, date, "005930"),
        )

        every { accountRepository.findById(1L) } returns Optional.of(account)
        every { portfolioSnapshotRepository.findByAccountIdAndBusinessDateOrderByTickerAsc(1L, date) } returns snapshots

        val result = service.getPortfolioSnapshots(1L, date)

        assertEquals(2, result.size)
        assertEquals("000660", result[0].ticker)
    }

    private fun account(): Account = Account.create(
        accountName = "test",
        accountType = AccountType.STOCK,
        tradingMode = TradingMode.LOCAL,
        initialDeposit = BigDecimal("1000000"),
    ).withId(1L)

    private fun snapshot(account: Account, date: LocalDate, ticker: String): PortfolioSnapshot = PortfolioSnapshot.create(
        account = account,
        businessDate = date,
        ticker = ticker,
        quantity = BigDecimal("1"),
        avgBuyPrice = BigDecimal("1000"),
        closePrice = BigDecimal("1100"),
        marketValue = BigDecimal("1100"),
        weight = BigDecimal("0.500000"),
        unrealizedPnl = BigDecimal("100"),
    )
}