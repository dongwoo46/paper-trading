package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.position.PositionExitTrigger
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param

interface PositionExitTriggerRepository : JpaRepository<PositionExitTrigger, Long> {
    fun findByPositionId(positionId: Long): PositionExitTrigger?
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(
        value = [
            QueryHint(
                name = "jakarta.persistence.lock.timeout",
                value = "3000", // 3초
            )
        ]
    )
    @Query("SELECT p FROM PositionExitTrigger p WHERE p.positionId = :positionId")
    fun findByPositionIdForUpdate(
        @Param("positionId") positionId: Long,
    ): PositionExitTrigger?
    fun findByTickerAndEnabledTrue(ticker: String): List<PositionExitTrigger>
}
