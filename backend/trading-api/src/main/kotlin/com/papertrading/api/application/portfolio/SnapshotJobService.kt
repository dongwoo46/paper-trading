package com.papertrading.api.application.portfolio

import com.papertrading.api.common.exception.AccountNotFoundException
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.portfolio.SnapshotJobRun
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.SnapshotJobRunRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class SnapshotJobService(
    private val accountRepository: AccountRepository,
    private val snapshotJobRunRepository: SnapshotJobRunRepository,
    private val dailyBalanceCommandService: DailyBalanceCommandService,
    private val portfolioSnapshotCommandService: PortfolioSnapshotCommandService,
) {
    @Transactional
    fun generateDailySnapshots(accountId: Long, businessDate: LocalDate): Int {
        val account = accountRepository.findByIdWithLock(accountId)
            .orElseThrow { AccountNotFoundException(accountId) }

        val run = startRun(account, businessDate)

        return try {
            dailyBalanceCommandService.recalculate(accountId, businessDate)
            val snapshots = portfolioSnapshotCommandService.recalculate(accountId, businessDate)
            run.complete()
            snapshotJobRunRepository.save(run)
            snapshots.size
        } catch (ex: PortfolioSnapshotDomainException) {
            run.fail(ex.message)
            snapshotJobRunRepository.save(run)
            throw ex
        } catch (ex: Exception) {
            run.fail(ex.message)
            snapshotJobRunRepository.save(run)
            throw SnapshotComputeFailedException("스냅샷 계산에 실패했습니다.", ex)
        }
    }

    private fun startRun(account: Account, businessDate: LocalDate): SnapshotJobRun {
        return try {
            snapshotJobRunRepository.save(SnapshotJobRun.start(account, businessDate))
        } catch (_: DataIntegrityViolationException) {
            throw SnapshotAlreadyRunningException(
                "이미 실행 중인 스냅샷 작업이 있습니다. accountId=${account.id} businessDate=$businessDate"
            )
        }
    }
}
