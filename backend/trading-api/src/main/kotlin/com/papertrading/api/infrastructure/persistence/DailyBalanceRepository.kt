package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.model.DailyBalance
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.Optional

interface DailyBalanceRepository : JpaRepository<DailyBalance, Long> {
    fun findByAccountIdAndBalanceDate(accountId: Long, date: LocalDate): Optional<DailyBalance>
    fun findByAccountIdAndBalanceDateBetweenOrderByBalanceDateAsc(
        accountId: Long,
        from: LocalDate,
        to: LocalDate
    ): List<DailyBalance>
}
