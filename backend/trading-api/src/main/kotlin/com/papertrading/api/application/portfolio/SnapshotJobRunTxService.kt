package com.papertrading.api.application.portfolio

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.portfolio.SnapshotJobRun
import com.papertrading.api.infrastructure.persistence.SnapshotJobRunRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class SnapshotJobRunTxService(
    private val snapshotJobRunRepository: SnapshotJobRunRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun start(account: Account, businessDate: LocalDate): SnapshotJobRun =
        snapshotJobRunRepository.saveAndFlush(SnapshotJobRun.start(account, businessDate))

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun complete(runId: Long) {
        val run = snapshotJobRunRepository.findById(runId)
            .orElseThrow { IllegalStateException("snapshot job run not found. runId=$runId") }
        run.complete()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun fail(runId: Long, message: String?) {
        val run = snapshotJobRunRepository.findById(runId)
            .orElseThrow { IllegalStateException("snapshot job run not found. runId=$runId") }
        run.fail(message)
    }
}

