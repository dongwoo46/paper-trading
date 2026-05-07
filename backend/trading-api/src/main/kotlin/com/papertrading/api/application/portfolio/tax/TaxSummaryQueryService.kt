package com.papertrading.api.application.portfolio.tax

import com.papertrading.api.domain.entity.portfolio.TaxSummary
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.TaxSummaryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaxSummaryQueryService(
    private val accountRepository: AccountRepository,
    private val taxSummaryRepository: TaxSummaryRepository,
) {

    @Transactional(readOnly = true)
    fun get(accountId: Long, taxYear: TaxYear): TaxSummary {
        if (!accountRepository.existsById(accountId)) {
            throw NoSuchElementException("계좌를 찾을 수 없습니다. id=$accountId")
        }

        return taxSummaryRepository.findByAccountIdAndTaxYear(accountId, taxYear.value)
            .orElseThrow { TaxSummaryNotFoundException("세금 요약을 찾을 수 없습니다. accountId=$accountId taxYear=${taxYear.value}") }
    }

    @Transactional(readOnly = true)
    fun list(accountId: Long, fromYear: TaxYear, toYear: TaxYear): List<TaxSummary> {
        if (fromYear.value > toYear.value) {
            throw InvalidTaxYearRangeException("fromYear는 toYear보다 클 수 없습니다. fromYear=${fromYear.value} toYear=${toYear.value}")
        }
        if (!accountRepository.existsById(accountId)) {
            throw NoSuchElementException("계좌를 찾을 수 없습니다. id=$accountId")
        }

        return taxSummaryRepository.findByAccountIdAndTaxYearBetweenOrderByTaxYearDesc(
            accountId = accountId,
            fromYear = fromYear.value,
            toYear = toYear.value,
        )
    }
}
