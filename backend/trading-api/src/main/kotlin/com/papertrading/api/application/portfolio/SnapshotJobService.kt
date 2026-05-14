package com.papertrading.api.application.portfolio

import com.papertrading.api.common.exception.AccountNotFoundException
import com.papertrading.api.common.exception.ApiDomainException
import com.papertrading.api.common.exception.SnapshotAlreadyRunningException
import com.papertrading.api.common.exception.SnapshotComputeFailedException
import com.papertrading.api.domain.enums.SnapshotJobRunStatus
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.SnapshotJobRunRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 특정 계좌의 특정 영업일 기준으로
 * 일별 잔고 및 포트폴리오 스냅샷을 재계산하고,
 * 작업 실행 상태를 기록하는 Job Command Service.
 *
 * scheduler에서 주기적으로 돌도록 해야함
 *
 * - 일반 사용자 Controller 직접 호출이 아니라 Scheduler 기반 실행을 기본으로 한다.
 * - 같은 accountId + businessDate 작업은 DB unique 제약(RUNNING 기준)으로 중복 실행을 방지한다.
 * - PostgreSQL 기준 partial unique index를 권장한다.
 *
 *   CREATE UNIQUE INDEX uk_snapshot_job_runs_running
 *   ON snapshot_job_runs(account_id, business_date)
 *   WHERE status = 'RUNNING';
 *
 * - saveAndFlush()를 사용하는 이유는 unique violation을 즉시 발생시켜
 *   SnapshotAlreadyRunningException으로 변환하기 위해서다.
 *
 * - JobRun은 실행 이력/중복 실행 방지 용도이며,
 *   실제 멱등성은 recalculate() 내부에서 보장해야 한다.
 * - 같은 accountId + businessDate로 여러 번 실행되어도
 *   최종 잔고/스냅샷 결과는 동일해야 한다.
 * - 재계산 데이터는 delete-then-insert 또는 upsert 방식 저장을 권장한다.
 *
 * - 현재 구조에서는 예외 발생 시 전체 트랜잭션 rollback으로 인해
 *   FAILED 상태가 DB에 남지 않을 수 있다.
 * - 실패 이력 보존이 필요하면 SnapshotJobRun 상태 변경을 별도 CommandService로 분리하고,
 *   start/complete/fail을 REQUIRES_NEW 트랜잭션으로 처리한다.
 *
 * - 애플리케이션 비정상 종료 시 RUNNING row가 남을 수 있으므로,
 *   운영 환경에서는 timeout 기반 복구 작업이 필요하다.
 */
@Service
class SnapshotJobService(
    private val accountRepository: AccountRepository,
    private val snapshotJobRunRepository: SnapshotJobRunRepository,
    private val snapshotJobRunTxService: SnapshotJobRunTxService,
    private val dailyBalanceCommandService: DailyBalanceCommandService,
    private val portfolioSnapshotCommandService: PortfolioSnapshotCommandService,
) {
    @Transactional
    fun generateDailySnapshots(
        accountId: Long,
        businessDate: LocalDate,
    ): Int {
        val account = accountRepository.findByIdWithLock(accountId)
            .orElseThrow { AccountNotFoundException(accountId) }

        // 재실행 정책: 같은 영업일의 마지막 실행이 SUCCESS면 멱등하게 재계산을 생략한다.
        val latestRun = snapshotJobRunRepository.findTopByAccountIdAndBusinessDateOrderByStartedAtDesc(accountId, businessDate)
        if (latestRun?.status == SnapshotJobRunStatus.SUCCESS) {
            return 0
        }

        val run = try {
            snapshotJobRunTxService.start(account, businessDate)
        } catch (_: DataIntegrityViolationException) {
            throw SnapshotAlreadyRunningException(
                "이미 실행 중인 스냅샷 작업이 있습니다. accountId=${account.id} businessDate=$businessDate"
            )
        }
        val runId = requireNotNull(run.id) { "snapshot job run id is null" }

        return try {
            dailyBalanceCommandService.recalculate(accountId, businessDate)

            val snapshots = portfolioSnapshotCommandService.recalculate(
                accountId,
                businessDate,
            )

            snapshotJobRunTxService.complete(runId)
            snapshots.size
        } catch (ex: ApiDomainException) {
            snapshotJobRunTxService.fail(runId, ex.message)
            throw ex
        } catch (ex: Exception) {
            snapshotJobRunTxService.fail(runId, ex.message)
            throw SnapshotComputeFailedException(
                "스냅샷 계산에 실패했습니다.",
                ex,
            )
        }
    }
}
