package com.papertrading.api.domain.position

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import jakarta.persistence.LockModeType

interface PositionExitTriggerRepository : JpaRepository<PositionExitTrigger, Long> {
    fun findByPositionId(positionId: Long): PositionExitTrigger?
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PositionExitTrigger p WHERE p.positionId = :positionId")
    fun findByPositionIdForUpdate(@Param("positionId") positionId: Long): PositionExitTrigger?
    fun findByTickerAndEnabledTrue(ticker: String): List<PositionExitTrigger>
}
