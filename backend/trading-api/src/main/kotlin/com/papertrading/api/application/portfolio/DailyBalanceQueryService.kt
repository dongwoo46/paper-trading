package com.papertrading.api.application.portfolio

import com.papertrading.api.domain.entity.portfolio.DailyBalance
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.DailyBalanceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class DailyBalanceQueryService(
    private val accountRepository: AccountRepository,
    private val dailyBalanceRepository: DailyBalanceRepository,
) {
    fun getDailyBalances(accountId: Long, fromDate: LocalDate, toDate: LocalDate): List<DailyBalance> {
        if (fromDate.isAfter(toDate)) {
            throw InvalidDateRangeException("fromDate는 toDate보다 이후일 수 없습니다. fromDate=$fromDate toDate=$toDate")
        }
        accountRepository.findById(accountId)
            .orElseThrow { NoSuchElementException("계좌를 찾을 수 없습니다. id=$accountId") }

        return dailyBalanceRepository.findByAccountIdAndBusinessDateBetweenOrderByBusinessDateAsc(
            accountId = accountId,
            from = fromDate,
            to = toDate,
        )
    }
}