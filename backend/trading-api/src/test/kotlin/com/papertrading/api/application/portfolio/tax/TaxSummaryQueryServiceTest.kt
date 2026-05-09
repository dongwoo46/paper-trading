package com.papertrading.api.application.portfolio.tax

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.TaxSummaryRepository
import com.papertrading.api.support.withId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.Optional

class TaxSummaryQueryServiceTest {
    private val accountRepository = mockk<AccountRepository>()
    private val taxSummaryRepository = mockk<TaxSummaryRepository>()
    private val service = TaxSummaryQueryService(accountRepository, taxSummaryRepository)

    @Test
    fun `get when account is LOCAL then returns summary`() {
        every { accountRepository.findById(1L) } returns Optional.of(localAccount())
        every { taxSummaryRepository.findByAccountIdAndTaxYear(1L, 2024) } returns
            Optional.of(sampleSummary(1L, 2024))

        val summary = service.get(1L, TaxYear(2024))

        assertEquals(2024, summary.taxYear)
    }

    @Test
    fun `list when account is LOCAL then returns summaries`() {
        every { accountRepository.findById(1L) } returns Optional.of(localAccount())
        every {
            taxSummaryRepository.findByAccountIdAndTaxYearBetweenOrderByTaxYearDesc(1L, 2023, 2024)
        } returns listOf(sampleSummary(1L, 2024), sampleSummary(1L, 2023))

        val summaries = service.list(1L, TaxYear(2023), TaxYear(2024))

        assertEquals(2, summaries.size)
        assertEquals(2024, summaries.first().taxYear)
    }

    @Test
    fun `get when account is KIS then throws invalid account mode`() {
        every { accountRepository.findById(1L) } returns Optional.of(kisAccount())

        assertThrows<InvalidAccountModeForTaxSummaryException> {
            service.get(1L, TaxYear(2024))
        }

        verify(exactly = 0) { taxSummaryRepository.findByAccountIdAndTaxYear(any(), any()) }
    }

    @Test
    fun `list when account is KIS then throws invalid account mode`() {
        every { accountRepository.findById(1L) } returns Optional.of(kisAccount())

        assertThrows<InvalidAccountModeForTaxSummaryException> {
            service.list(1L, TaxYear(2023), TaxYear(2024))
        }

        verify(exactly = 0) { taxSummaryRepository.findByAccountIdAndTaxYearBetweenOrderByTaxYearDesc(any(), any(), any()) }
    }

    private fun kisAccount(): Account = Account.create(
        accountName = "kis",
        accountType = AccountType.STOCK,
        tradingMode = TradingMode.KIS_LIVE,
        initialDeposit = BigDecimal("1000000"),
    ).withId(1L)

    private fun localAccount(): Account = Account.create(
        accountName = "local",
        accountType = AccountType.STOCK,
        tradingMode = TradingMode.LOCAL,
        initialDeposit = BigDecimal("1000000"),
    ).withId(1L)

    private fun sampleSummary(accountId: Long, taxYear: Int): com.papertrading.api.domain.entity.portfolio.TaxSummary {
        val account = localAccount()
        val field = account.javaClass.getDeclaredField("id")
        field.isAccessible = true
        field.set(account, accountId)

        return com.papertrading.api.domain.entity.portfolio.TaxSummary.create(
            account = account,
            taxYear = taxYear,
            totalRealizedPnl = BigDecimal("100.0000"),
            taxablePnl = BigDecimal("80.0000"),
            estimatedTax = BigDecimal("12.0000"),
        )
    }
}
