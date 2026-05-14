package com.papertrading.api.application.portfolio.tax

import com.papertrading.api.application.portfolio.TaxSummaryCommandService
import com.papertrading.api.common.exception.InvalidAccountModeForTaxSummaryException
import com.papertrading.api.common.exception.TaxSummaryAlreadyRunningException
import com.papertrading.api.common.exception.TaxSummaryComputeFailedException
import com.papertrading.api.common.exception.TaxYearNotClosedException
import com.papertrading.api.common.exception.UnsupportedCurrencyException
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.portfolio.TaxSummary
import com.papertrading.api.domain.entity.portfolio.TaxSummaryRun
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TaxSummaryRunType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.TaxSummaryRepository
import com.papertrading.api.infrastructure.persistence.TaxSummaryRunRepository
import com.papertrading.api.support.withId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DataIntegrityViolationException
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional

class TaxSummaryCommandServiceTest {

    private val accountRepository = mockk<AccountRepository>()
    private val taxSummaryRepository = mockk<TaxSummaryRepository>()
    private val taxSummaryRunRepository = mockk<TaxSummaryRunRepository>()
    private val settlementTaxReadRepository = mockk<SettlementTaxReadRepository>()
    private val clock = Clock.fixed(Instant.parse("2026-01-10T00:00:00Z"), ZoneOffset.UTC)

    private val service = TaxSummaryCommandService(
        accountRepository,
        taxSummaryRepository,
        taxSummaryRunRepository,
        settlementTaxReadRepository,
        clock,
    )

    private fun account(): Account = Account.create(
        accountName = "test",
        accountType = AccountType.STOCK,
        tradingMode = TradingMode.LOCAL,
        initialDeposit = BigDecimal("1000000")
    ).withId(1L)

    private fun kisAccount(): Account = Account.create(
        accountName = "kis",
        accountType = AccountType.STOCK,
        tradingMode = TradingMode.KIS_PAPER,
        initialDeposit = BigDecimal("1000000")
    ).withId(1L)

    @Test
    fun `recalculate success creates summary and marks run success`() {
        val account = account()

        every { taxSummaryRunRepository.existsRunning(1L, 2024) } returns false
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { settlementTaxReadRepository.summarizeForTax(eq(1L), any(), any()) } returns
            TaxSettlementAggregate(BigDecimal("1000"), BigDecimal("100"), BigDecimal("50"), "KRW")
        every { taxSummaryRepository.findByAccountIdAndTaxYear(1L, 2024) } returns Optional.empty()
        every { taxSummaryRunRepository.save(any<TaxSummaryRun>()) } answers { firstArg() }
        every { taxSummaryRepository.save(any<TaxSummary>()) } answers { firstArg() }

        val summary = service.recalculate(1L, TaxYear(2024), force = false)

        assertEquals(BigDecimal("1000.0000"), summary.totalRealizedPnl)
        assertEquals(BigDecimal("850.0000"), summary.taxablePnl)
        assertEquals(BigDecimal("50.0000"), summary.estimatedTax)
        verify(exactly = 2) { taxSummaryRunRepository.save(any<TaxSummaryRun>()) }
        verify { taxSummaryRunRepository.save(match<TaxSummaryRun> { it.runType == TaxSummaryRunType.MANUAL }) }
    }

    @Test
    fun `recalculate blocks duplicate running`() {
        every { taxSummaryRunRepository.existsRunning(1L, 2024) } returns true

        assertThrows<TaxSummaryAlreadyRunningException> {
            service.recalculate(1L, TaxYear(2024), force = false)
        }
    }

    @Test
    fun `recalculate maps running unique constraint race to domain exception`() {
        val account = account()

        every { taxSummaryRunRepository.existsRunning(1L, 2024) } returns false
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { taxSummaryRunRepository.save(any<TaxSummaryRun>()) } throws
            DataIntegrityViolationException("uq_tax_summary_runs_running_account_year")

        assertThrows<TaxSummaryAlreadyRunningException> {
            service.recalculate(1L, TaxYear(2024), force = false)
        }

        verify(exactly = 1) { taxSummaryRunRepository.save(any<TaxSummaryRun>()) }
    }

    @Test
    fun `recalculate failure saves failed run`() {
        val account = account()

        every { taxSummaryRunRepository.existsRunning(1L, 2024) } returns false
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { settlementTaxReadRepository.summarizeForTax(eq(1L), any(), any()) } throws RuntimeException("boom")
        every { taxSummaryRunRepository.save(any<TaxSummaryRun>()) } answers { firstArg() }

        assertThrows<TaxSummaryComputeFailedException> {
            service.recalculate(1L, TaxYear(2024), force = true)
        }

        verify(exactly = 2) { taxSummaryRunRepository.save(any<TaxSummaryRun>()) }
        verify(exactly = 0) { taxSummaryRepository.save(any<TaxSummary>()) }
    }

    @Test
    fun `recalculate when mixed currency then throws unsupported currency`() {
        val account = account()

        every { taxSummaryRunRepository.existsRunning(1L, 2024) } returns false
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { settlementTaxReadRepository.summarizeForTax(eq(1L), any(), any()) } returns
            TaxSettlementAggregate(BigDecimal("1000"), BigDecimal("100"), BigDecimal("50"), "USD")
        every { taxSummaryRunRepository.save(any<TaxSummaryRun>()) } answers { firstArg() }

        assertThrows<UnsupportedCurrencyException> {
            service.recalculate(1L, TaxYear(2024), force = true)
        }

        verify(exactly = 2) { taxSummaryRunRepository.save(any<TaxSummaryRun>()) }
        verify(exactly = 0) { taxSummaryRepository.save(any<TaxSummary>()) }
    }

    @Test
    fun `recalculate with batch run type records YEAR_END_BATCH`() {
        val account = account()

        every { taxSummaryRunRepository.existsRunning(1L, 2024) } returns false
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { settlementTaxReadRepository.summarizeForTax(eq(1L), any(), any()) } returns
            TaxSettlementAggregate(BigDecimal("1000"), BigDecimal("100"), BigDecimal("50"), "KRW")
        every { taxSummaryRepository.findByAccountIdAndTaxYear(1L, 2024) } returns Optional.empty()
        every { taxSummaryRunRepository.save(any<TaxSummaryRun>()) } answers { firstArg() }
        every { taxSummaryRepository.save(any<TaxSummary>()) } answers { firstArg() }

        service.recalculate(1L, TaxYear(2024), force = true, runType = TaxSummaryRunType.YEAR_END_BATCH)

        verify { taxSummaryRunRepository.save(match<TaxSummaryRun> { it.runType == TaxSummaryRunType.YEAR_END_BATCH }) }
    }

    @Test
    fun `recalculate when account is KIS then throws invalid account mode`() {
        val account = kisAccount()
        every { taxSummaryRunRepository.existsRunning(1L, 2024) } returns false
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)

        val ex = assertThrows<InvalidAccountModeForTaxSummaryException> {
            service.recalculate(1L, TaxYear(2024), force = true)
        }

        assertEquals("INVALID_ACCOUNT_MODE_FOR_TAX_SUMMARY", ex.errorCode)
        verify(exactly = 0) { taxSummaryRunRepository.save(any<TaxSummaryRun>()) }
    }

    @Test
    fun `recalculate when not force and current year then throws not closed`() {
        assertThrows<TaxYearNotClosedException> {
            service.recalculate(1L, TaxYear(2026), force = false)
        }
    }
}
