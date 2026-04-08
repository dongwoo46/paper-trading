package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.model.Account
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface AccountRepository : JpaRepository<Account, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    fun findByIdWithLock(@Param("id") id: Long): Optional<Account>

    fun findByTradingModeAndIsActiveTrue(tradingMode: TradingMode): List<Account>
}
