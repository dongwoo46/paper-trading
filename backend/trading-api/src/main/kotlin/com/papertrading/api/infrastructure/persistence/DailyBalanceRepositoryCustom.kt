package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.portfolio.DailyBalance
import java.time.LocalDate

interface DailyBalanceRepositoryCustom {
    fun searchDailyBalances(accountId: Long, from: LocalDate, to: LocalDate): List<DailyBalance>
}

