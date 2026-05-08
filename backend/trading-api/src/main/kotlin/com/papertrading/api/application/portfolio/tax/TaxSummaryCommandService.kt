package com.papertrading.api.application.portfolio.tax

import com.papertrading.api.domain.entity.portfolio.TaxSummary
import com.papertrading.api.domain.entity.portfolio.TaxSummaryRun
import com.papertrading.api.domain.enums.TaxSummaryRunType
import com.papertrading.api.common.exception.AccountNotFoundException
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.TaxSummaryRepository
import com.papertrading.api.infrastructure.persistence.TaxSummaryRunRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Service
class TaxSummaryCommandService(
    private val accountRepository: AccountRepository,
    private val taxSummaryRepository: TaxSummaryRepository,
    private val taxSummaryRunRepository: TaxSummaryRunRepository,
    private val settlementTaxReadRepository: SettlementTaxReadRepository,
    private val taxSummaryCalculator: TaxSummaryCalculator,
    private val clock: Clock = Clock.systemUTC(),
) {

    @Transactional
    fun recalculate(
        accountId: Long,
        taxYear: TaxYear,
        force: Boolean,
        runType: TaxSummaryRunType = TaxSummaryRunType.MANUAL,
    ): TaxSummary {
        val currentYear = Instant.now(clock).atZone(ZoneOffset.UTC).year
        if (!force && taxYear.value >= currentYear) {
            throw TaxYearNotClosedException("종료되지 않은 과세연도는 강제 재계산(force=true)만 가능합니다. taxYear=${taxYear.value}")
        }

        if (taxSummaryRunRepository.existsRunning(accountId, taxYear.value)) {
            throw TaxSummaryAlreadyRunningException("이미 실행 중인 세금 요약 작업이 있습니다. accountId=$accountId taxYear=${taxYear.value}")
        }

        val account = accountRepository.findByIdWithLock(accountId)
            .orElseThrow { AccountNotFoundException(accountId) }

        val run = try {
            taxSummaryRunRepository.save(TaxSummaryRun.start(account, taxYear.value, runType))
        } catch (ex: DataIntegrityViolationException) {
            throw TaxSummaryAlreadyRunningException(
                "이미 실행 중인 세금 요약 작업이 있습니다. accountId=$accountId taxYear=${taxYear.value}"
            )
        }

        return try {
            val aggregate = settlementTaxReadRepository.summarizeForTax(
                accountId = accountId,
                yearStart = taxYear.startInclusive(),
                yearEnd = taxYear.endExclusive(),
            )
            val result = taxSummaryCalculator.compute(aggregate)

            val summary = taxSummaryRepository.findByAccountIdAndTaxYear(accountId, taxYear.value)
                .map {
                    it.recalculate(result.totalRealizedPnl, result.taxablePnl, result.estimatedTax)
                    it
                }
                .orElseGet {
                    TaxSummary.create(
                        account = account,
                        taxYear = taxYear.value,
                        totalRealizedPnl = result.totalRealizedPnl,
                        taxablePnl = result.taxablePnl,
                        estimatedTax = result.estimatedTax,
                    )
                }

            val saved = taxSummaryRepository.save(summary)
            run.completeSuccess()
            taxSummaryRunRepository.save(run)
            saved
        } catch (ex: TaxSummaryDomainException) {
            run.fail(ex.message)
            taxSummaryRunRepository.save(run)
            throw ex
        } catch (ex: IllegalArgumentException) {
            val wrapped = UnsupportedCurrencyException(ex.message ?: "지원하지 않는 통화입니다.")
            run.fail(wrapped.message)
            taxSummaryRunRepository.save(run)
            throw wrapped
        } catch (ex: Exception) {
            val wrapped = TaxSummaryComputeFailedException("세금 요약 계산에 실패했습니다.", ex)
            run.fail(wrapped.message)
            taxSummaryRunRepository.save(run)
            throw wrapped
        }
    }
}
