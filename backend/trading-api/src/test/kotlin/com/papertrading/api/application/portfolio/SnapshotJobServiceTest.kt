package com.papertrading.api.application.portfolio

import com.papertrading.api.common.exception.SnapshotAlreadyRunningException
import com.papertrading.api.common.exception.SnapshotComputeFailedException
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.portfolio.SnapshotJobRun
import com.papertrading.api.domain.entity.portfolio.SnapshotJobRunStatus
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.SnapshotJobRunRepository
import com.papertrading.api.support.withId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class SnapshotJobServiceTest {
    private val accountRepository = mockk<AccountRepository>()
    private val snapshotJobRunRepository = mockk<SnapshotJobRunRepository>()
    private val dailyBalanceCommandService = mockk<DailyBalanceCommandService>()
    private val portfolioSnapshotCommandService = mockk<PortfolioSnapshotCommandService>()

    private val service = SnapshotJobService(
        accountRepository,
        snapshotJobRunRepository,
        dailyBalanceCommandService,
        portfolioSnapshotCommandService,
    )

    @Test
    fun `generateDailySnapshots throws SNAPSHOT_ALREADY_RUNNING on duplicate run`() {
        val account = account()
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { snapshotJobRunRepository.save(any<SnapshotJobRun>()) } throws DataIntegrityViolationException("uq")

        val ex = assertThrows(SnapshotAlreadyRunningException::class.java) {
            service.generateDailySnapshots(1L, LocalDate.of(2026, 5, 1))
        }

        assertEquals("SNAPSHOT_ALREADY_RUNNING", ex.errorCode)
    }

    @Test
    fun `generateDailySnapshots maps failure to SNAPSHOT_COMPUTE_FAILED and keeps run failed`() {
        val account = account()
        val running = SnapshotJobRun.start(account, LocalDate.of(2026, 5, 1))
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { snapshotJobRunRepository.save(any<SnapshotJobRun>()) } answers { firstArg() }
        every { dailyBalanceCommandService.recalculate(1L, LocalDate.of(2026, 5, 1)) } throws RuntimeException("boom")

        val ex = assertThrows(SnapshotComputeFailedException::class.java) {
            service.generateDailySnapshots(1L, LocalDate.of(2026, 5, 1))
        }

        assertEquals("SNAPSHOT_COMPUTE_FAILED", ex.errorCode)
        verify(exactly = 2) { snapshotJobRunRepository.save(any<SnapshotJobRun>()) }
    }

    @Test
    fun `generateDailySnapshots is transactional atomic boundary`() {
        val transactional = SnapshotJobService::class.java
            .getDeclaredMethod("generateDailySnapshots", Long::class.java, LocalDate::class.java)
            .getAnnotation(Transactional::class.java)
        org.junit.jupiter.api.Assertions.assertNotNull(transactional)
    }

    private fun account(): Account = Account.create(
        accountName = "test",
        accountType = AccountType.STOCK,
        tradingMode = TradingMode.LOCAL,
        initialDeposit = BigDecimal("1000000"),
    ).withId(1L)
}
