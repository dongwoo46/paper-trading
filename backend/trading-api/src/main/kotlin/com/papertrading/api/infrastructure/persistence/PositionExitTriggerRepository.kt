package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.position.PositionExitTrigger
import com.papertrading.api.domain.enums.TriggerState
import com.papertrading.api.domain.enums.TriggerType
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param

interface PositionExitTriggerRepository : JpaRepository<PositionExitTrigger, Long> {
    fun findAllByPositionIdOrderByIdAsc(positionId: Long): List<PositionExitTrigger>

    fun findByIdAndPositionId(id: Long, positionId: Long): PositionExitTrigger?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(
        value = [
            QueryHint(
                name = "jakarta.persistence.lock.timeout",
                value = "3000", // 3초
            )
        ]
    )
    @Query("SELECT p FROM PositionExitTrigger p WHERE p.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: Long,
    ): PositionExitTrigger?

    fun findByTickerAndState(ticker: String, state: TriggerState): List<PositionExitTrigger>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(
        value = [
            QueryHint(
                name = "jakarta.persistence.lock.timeout",
                value = "3000",
            )
        ]
    )
    @Query(
        """
        SELECT p
        FROM PositionExitTrigger p
        WHERE p.ticker = :ticker
          AND p.positionId = :positionId
          AND p.triggerType = :triggerType
          AND p.state = com.papertrading.api.domain.enums.TriggerState.ARMED
        ORDER BY p.id ASC
        """
    )
    fun findArmedGroupForUpdate(
        @Param("ticker") ticker: String,
        @Param("positionId") positionId: Long,
        @Param("triggerType") triggerType: TriggerType,
    ): List<PositionExitTrigger>
}
