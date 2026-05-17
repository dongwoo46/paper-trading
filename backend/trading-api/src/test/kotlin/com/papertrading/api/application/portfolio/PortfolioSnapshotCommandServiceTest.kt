package com.papertrading.api.application.portfolio

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.portfolio.PortfolioSnapshot
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.port.MarketQuotePort
import com.papertrading.api.application.common.result.QuoteSnapshot
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.PortfolioSnapshotRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import com.papertrading.api.support.withId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Optional

class PortfolioSnapshotCommandServiceTest {
    private val accountRepository = mockk<AccountRepository>()
    private val positionRepository = mockk<PositionRepository>()
    private val portfolioSnapshotRepository = mockk<PortfolioSnapshotRepository>()
    private val marketQuotePort = mockk<MarketQuotePort>()

    private val service = PortfolioSnapshotCommandService(
        accountRepository = accountRepository,
        positionRepository = positionRepository,
        portfolioSnapshotRepository = portfolioSnapshotRepository,
        marketQuotePort = marketQuotePort,
    )

    @Test
    fun `recalculate computes ticker weights and upserts by account-date-ticker`() {
        val account = account()
        val businessDate = LocalDate.of(2026, 5, 6)
        val a = Position.createWithHolding(
            account = account,
            ticker = "005930",
            marketType = MarketType.KOSPI,
            quantity = BigDecimal("10"),
            avgBuyPrice = BigDecimal("65000"),
        )
        val b = Position.createWithHolding(
            account = account,
            ticker = "000660",
            marketType = MarketType.KOSPI,
            quantity = BigDecimal("5"),
            avgBuyPrice = BigDecimal("200000"),
        )
        val existingA = PortfolioSnapshot.create(
            account = account,
            businessDate = businessDate,
            ticker = "005930",
            quantity = BigDecimal.ONE,
            avgBuyPrice = BigDecimal.ONE,
            closePrice = BigDecimal.ONE,
            marketValue = BigDecimal.ONE,
            weight = BigDecimal.ONE,
            unrealizedPnl = BigDecimal.ONE,
        )

        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { positionRepository.findByAccountIdAndQuantityGreaterThan(1L, BigDecimal.ZERO) } returns listOf(a, b)
        every { marketQuotePort.getQuote(TradingMode.LOCAL, "005930") } returns quote("005930", "70000")
        every { marketQuotePort.getQuote(TradingMode.LOCAL, "000660") } returns quote("000660", "210000")
        every {
            portfolioSnapshotRepository.findByAccountIdAndBusinessDateAndTicker(1L, businessDate, "005930")
        } returns Optional.of(existingA)
        every {
            portfolioSnapshotRepository.findByAccountIdAndBusinessDateAndTicker(1L, businessDate, "000660")
        } returns Optional.empty()
        every { portfolioSnapshotRepository.save(any<PortfolioSnapshot>()) } answers { firstArg() }

        val results = service.recalculate(1L, businessDate)

        assertEquals(2, results.size)
        val rA = results.first { it.ticker == "005930" }
        val rB = results.first { it.ticker == "000660" }
        assertEquals(BigDecimal("700000"), rA.marketValue)
        assertEquals(BigDecimal("0.400000"), rA.weight)
        assertEquals(BigDecimal("50000"), rA.unrealizedPnl)
        assertEquals(BigDecimal("1050000"), rB.marketValue)
        assertEquals(BigDecimal("0.600000"), rB.weight)
        verify(exactly = 2) { portfolioSnapshotRepository.save(any<PortfolioSnapshot>()) }
    }

    private fun account(): Account = Account.create(
        accountName = "test",
        accountType = AccountType.STOCK,
        tradingMode = TradingMode.LOCAL,
        initialDeposit = BigDecimal("1000000"),
    ).withId(1L)

    private fun quote(ticker: String, price: String): QuoteSnapshot = QuoteSnapshot(
        ticker = ticker,
        tradingMode = TradingMode.LOCAL,
        price = BigDecimal(price),
        askp1 = BigDecimal(price),
        bidp1 = BigDecimal(price),
        updatedAt = Instant.parse("2026-05-06T06:00:00Z"),
    )
}
