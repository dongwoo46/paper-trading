package com.papertrading.api.presentation.controller

import com.papertrading.api.application.portfolio.tax.TaxSummaryBatchService
import com.papertrading.api.application.portfolio.tax.TaxSummaryCommandService
import com.papertrading.api.application.portfolio.tax.TaxSummaryQueryService
import com.papertrading.api.application.portfolio.tax.TaxYear
import com.papertrading.api.presentation.dto.portfolio.RecalculateTaxSummaryRequest
import com.papertrading.api.presentation.dto.portfolio.TaxSummaryResponse
import com.papertrading.api.presentation.dto.portfolio.YearEndBatchRequest
import com.papertrading.api.presentation.dto.portfolio.YearEndBatchResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class TaxSummaryController(
    private val taxSummaryQueryService: TaxSummaryQueryService,
    private val taxSummaryCommandService: TaxSummaryCommandService,
    private val taxSummaryBatchService: TaxSummaryBatchService,
) {

    @GetMapping("/accounts/{accountId}/tax-summaries/{taxYear}")
    fun getTaxSummary(
        @PathVariable accountId: Long,
        @PathVariable taxYear: Int,
    ): TaxSummaryResponse = TaxSummaryResponse.from(
        taxSummaryQueryService.get(accountId, TaxYear(taxYear))
    )

    @GetMapping("/accounts/{accountId}/tax-summaries")
    fun listTaxSummaries(
        @PathVariable accountId: Long,
        @RequestParam fromYear: Int,
        @RequestParam toYear: Int,
    ): List<TaxSummaryResponse> = taxSummaryQueryService.list(
        accountId = accountId,
        fromYear = TaxYear(fromYear),
        toYear = TaxYear(toYear),
    ).map { TaxSummaryResponse.from(it) }

    @PostMapping("/accounts/{accountId}/tax-summaries/{taxYear}/recalculate")
    fun recalculateTaxSummary(
        @PathVariable accountId: Long,
        @PathVariable taxYear: Int,
        @RequestBody(required = false) request: RecalculateTaxSummaryRequest?,
    ): TaxSummaryResponse = TaxSummaryResponse.from(
        taxSummaryCommandService.recalculate(accountId, TaxYear(taxYear), force = request?.force ?: false)
    )

    @PostMapping("/tax-summaries/jobs/year-end")
    fun triggerYearEndBatch(@Valid @RequestBody request: YearEndBatchRequest): YearEndBatchResponse {
        val year = TaxYear(requireNotNull(request.taxYear) { "taxYear는 필수입니다." })
        val count = taxSummaryBatchService.runYearEnd(year, request.accountIds)
        return YearEndBatchResponse(taxYear = year.value, requestedAccounts = count)
    }
}
