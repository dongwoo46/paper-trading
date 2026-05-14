package com.papertrading.api.application.portfolio

import com.papertrading.api.common.exception.AccountNotFoundException
import com.papertrading.api.common.exception.InvalidDateRangeException
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
    // 특정 계좌의 일별 잔고 히스토리 조회
    fun getDailyBalances(accountId: Long, fromDate: LocalDate, toDate: LocalDate): List<DailyBalance> {
        if (fromDate.isAfter(toDate)) {
            throw InvalidDateRangeException("fromDate는 toDate보다 이후일 수 없습니다. fromDate=$fromDate toDate=$toDate")
        }
        accountRepository.findById(accountId)
            .orElseThrow { AccountNotFoundException(accountId) }

        return dailyBalanceRepository.searchDailyBalances(
            accountId = accountId,
            from = fromDate,
            to = toDate,
        )
    }
}
