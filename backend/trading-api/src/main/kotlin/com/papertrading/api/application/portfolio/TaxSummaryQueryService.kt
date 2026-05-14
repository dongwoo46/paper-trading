package com.papertrading.api.application.portfolio

import com.papertrading.api.application.portfolio.tax.TaxYear
import com.papertrading.api.common.exception.AccountNotFoundException
import com.papertrading.api.common.exception.InvalidAccountModeForTaxSummaryException
import com.papertrading.api.common.exception.InvalidTaxYearRangeException
import com.papertrading.api.common.exception.TaxSummaryNotFoundException
import com.papertrading.api.domain.entity.portfolio.TaxSummary
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.TaxSummaryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TaxSummaryQueryService(
    private val accountRepository: AccountRepository,
    private val taxSummaryRepository: TaxSummaryRepository,
) {

    fun get(accountId: Long, taxYear: TaxYear): TaxSummary {
        val account = accountRepository.findById(accountId)
            .orElseThrow { AccountNotFoundException(accountId) }
        ensureLocalMode(accountId, account.tradingMode)

        return taxSummaryRepository.findOneByAccountIdAndTaxYear(accountId, taxYear.value)
            .orElseThrow { TaxSummaryNotFoundException("세금 요약을 찾을 수 없습니다. accountId=$accountId taxYear=${taxYear.value}") }
    }

    fun list(accountId: Long, fromYear: TaxYear, toYear: TaxYear): List<TaxSummary> {
        if (fromYear.value > toYear.value) {
            throw InvalidTaxYearRangeException("fromYear는 toYear보다 클 수 없습니다. fromYear=${fromYear.value} toYear=${toYear.value}")
        }
        val account = accountRepository.findById(accountId)
            .orElseThrow { AccountNotFoundException(accountId) }
        ensureLocalMode(accountId, account.tradingMode)

        return taxSummaryRepository.searchByAccountIdAndTaxYearRange(
            accountId = accountId,
            fromYear = fromYear.value,
            toYear = toYear.value,
        )
    }

    private fun ensureLocalMode(accountId: Long, tradingMode: TradingMode) {
        if (tradingMode != TradingMode.LOCAL) {
            throw InvalidAccountModeForTaxSummaryException(
                "TaxSummary는 LOCAL 계좌만 지원합니다. accountId=$accountId tradingMode=$tradingMode"
            )
        }
    }
}
