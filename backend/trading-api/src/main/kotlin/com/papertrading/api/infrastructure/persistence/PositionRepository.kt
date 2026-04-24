package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.model.Position
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.util.Optional

interface PositionRepository : JpaRepository<Position, Long> {
    fun findByAccountIdAndTicker(accountId: Long, ticker: String): Optional<Position>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Position p WHERE p.account.id = :accountId AND p.ticker = :ticker")
    fun findByAccountIdAndTickerWithLock(
        @Param("accountId") accountId: Long,
        @Param("ticker") ticker: String
    ): Optional<Position>

    fun findByAccountIdAndQuantityGreaterThan(accountId: Long, minQuantity: BigDecimal): List<Position>

    // ticker 기준 보유 포지션 전체 조회 (QuoteEventListener 시세 갱신용)
    fun findByTickerAndQuantityGreaterThan(ticker: String, minQuantity: BigDecimal): List<Position>
}
