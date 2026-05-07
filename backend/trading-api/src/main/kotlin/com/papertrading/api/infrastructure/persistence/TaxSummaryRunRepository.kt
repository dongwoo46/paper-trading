package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.portfolio.TaxSummaryRun
import com.papertrading.api.domain.enums.TaxSummaryRunStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TaxSummaryRunRepository : JpaRepository<TaxSummaryRun, Long> {
    @Query(
        """
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
        FROM TaxSummaryRun r
        WHERE r.account.id = :accountId
          AND r.taxYear = :taxYear
          AND r.status = :status
        """
    )
    fun existsRunning(
        @Param("accountId") accountId: Long,
        @Param("taxYear") taxYear: Int,
        @Param("status") status: TaxSummaryRunStatus = TaxSummaryRunStatus.RUNNING,
    ): Boolean
}
