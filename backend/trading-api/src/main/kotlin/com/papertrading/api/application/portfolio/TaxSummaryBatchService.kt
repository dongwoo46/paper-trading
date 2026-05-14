package com.papertrading.api.application.portfolio

import com.papertrading.api.application.portfolio.tax.TaxYear
import com.papertrading.api.common.exception.TaxYearNotClosedException
import com.papertrading.api.domain.enums.TaxSummaryRunType
import com.papertrading.api.infrastructure.persistence.AccountRepository
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Service
class TaxSummaryBatchService(
    private val accountRepository: AccountRepository,
    private val taxSummaryCommandService: TaxSummaryCommandService,
    private val clock: Clock = Clock.systemUTC(),
) {

    fun runYearEnd(taxYear: TaxYear, accountIds: List<Long>?): Int {
        val currentYear = Instant.now(clock).atZone(ZoneOffset.UTC).year
        if (taxYear.value >= currentYear) {
            throw TaxYearNotClosedException("종료되지 않은 과세연도는 배치 실행할 수 없습니다. taxYear=${taxYear.value}")
        }

        val targetAccountIds = accountIds
            ?.filterNotNull()
            ?.distinct()
            ?.ifEmpty { accountRepository.findByIsActiveTrue().mapNotNull { it.id } }
            ?: accountRepository.findByIsActiveTrue().mapNotNull { it.id }

        targetAccountIds.forEach { accountId ->
            taxSummaryCommandService.recalculate(
                accountId = accountId,
                taxYear = taxYear,
                force = true,
                runType = TaxSummaryRunType.YEAR_END_BATCH,
            )
        }

        return targetAccountIds.size
    }
}
