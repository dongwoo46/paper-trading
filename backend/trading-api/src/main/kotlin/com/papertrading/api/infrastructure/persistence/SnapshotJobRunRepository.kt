package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.portfolio.SnapshotJobRun
import com.papertrading.api.domain.entity.portfolio.SnapshotJobRunStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface SnapshotJobRunRepository : JpaRepository<SnapshotJobRun, Long> {
    @Query(
        """
        select case when count(r) > 0 then true else false end
        from SnapshotJobRun r
        where r.account.id = :accountId and r.businessDate = :businessDate and r.status = :status
        """
    )
    fun existsByAccountIdAndBusinessDateAndStatus(
        @Param("accountId") accountId: Long,
        @Param("businessDate") businessDate: LocalDate,
        @Param("status") status: SnapshotJobRunStatus,
    ): Boolean
}