package com.papertrading.api.application.portfolio

import com.papertrading.api.application.portfolio.result.TaxComputationResult
import com.papertrading.api.application.portfolio.tax.SettlementTaxReadRepository
import com.papertrading.api.application.portfolio.tax.TaxSettlementAggregate
import com.papertrading.api.application.portfolio.tax.TaxYear
import com.papertrading.api.common.exception.AccountNotFoundException
import com.papertrading.api.common.exception.ApiDomainException
import com.papertrading.api.common.exception.InvalidAccountModeForTaxSummaryException
import com.papertrading.api.common.exception.TaxSummaryAlreadyRunningException
import com.papertrading.api.common.exception.TaxSummaryComputeFailedException
import com.papertrading.api.common.exception.TaxYearNotClosedException
import com.papertrading.api.common.exception.UnsupportedCurrencyException
import com.papertrading.api.domain.entity.portfolio.TaxSummary
import com.papertrading.api.domain.entity.portfolio.TaxSummaryRun
import com.papertrading.api.domain.enums.TaxSummaryRunType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.TaxSummaryRepository
import com.papertrading.api.infrastructure.persistence.TaxSummaryRunRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

// 세금 요약은 Settlement 집계(DB)를 기준으로 계산한다.
// 시장별(국장/미장) 거래세율 차이는 체결 시점 FeePolicy로 settlement.tax에 반영되며,
// 연간 계산에서는 고정 상수 대신 해당 집계값을 사용한다.
@Service
class TaxSummaryCommandService(
    private val accountRepository: AccountRepository,
    private val taxSummaryRepository: TaxSummaryRepository,
    private val taxSummaryRunRepository: TaxSummaryRunRepository,
    private val settlementTaxReadRepository: SettlementTaxReadRepository,
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
        if (account.tradingMode != TradingMode.LOCAL) {
            throw InvalidAccountModeForTaxSummaryException(
                "TaxSummary는 LOCAL 계좌만 지원합니다. accountId=$accountId tradingMode=${account.tradingMode}"
            )
        }

        val run = try {
            taxSummaryRunRepository.save(TaxSummaryRun.Companion.start(account, taxYear.value, runType))
        } catch (ex: DataIntegrityViolationException) {
            throw TaxSummaryAlreadyRunningException(
                "이미 실행 중인 세금 요약 작업이 있습니다. accountId=$accountId taxYear=${taxYear.value}"
            )
        }

        return try {
            val aggregate = settlementTaxReadRepository.summarizeForTax(
                accountId = accountId,
                yearStartInclusive = taxYear.startInclusive(),
                yearEndExclusive = taxYear.endExclusive(),
            )
            val result = compute(aggregate)

            val summary = taxSummaryRepository.findByAccountIdAndTaxYear(accountId, taxYear.value)
                .map {
                    it.recalculate(result.totalRealizedPnl, result.taxablePnl, result.estimatedTax)
                    it
                }
                .orElseGet {
                    TaxSummary.Companion.create(
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
        } catch (ex: ApiDomainException) {
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

    private fun compute(input: TaxSettlementAggregate): TaxComputationResult {
        if (input.currency != "KRW") {
            throw UnsupportedCurrencyException("지원하지 않는 통화입니다. currency=${input.currency}")
        }

        val totalRealized = input.totalRealizedPnl.setScale(4, RoundingMode.HALF_UP)
        val taxable = totalRealized.subtract(input.totalFee).subtract(input.totalTax).setScale(4, RoundingMode.HALF_UP)
        val taxableNonNegative = taxable.max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)
        val estimated = input.totalTax.max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)

        return TaxComputationResult.of(
            totalRealizedPnl = totalRealized,
            taxablePnl = taxableNonNegative,
            estimatedTax = estimated
        )
    }
}
