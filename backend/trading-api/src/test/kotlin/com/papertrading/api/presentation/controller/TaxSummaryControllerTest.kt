package com.papertrading.api.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.api.application.portfolio.tax.InvalidTaxYearRangeException
import com.papertrading.api.application.portfolio.tax.InvalidAccountModeForTaxSummaryException
import com.papertrading.api.application.portfolio.tax.TaxSummaryAlreadyRunningException
import com.papertrading.api.application.portfolio.tax.TaxSummaryBatchService
import com.papertrading.api.application.portfolio.tax.TaxSummaryCommandService
import com.papertrading.api.application.portfolio.tax.TaxSummaryNotFoundException
import com.papertrading.api.application.portfolio.tax.TaxSummaryQueryService
import com.papertrading.api.application.portfolio.tax.TaxYear
import com.papertrading.api.application.portfolio.tax.UnsupportedCurrencyException
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.portfolio.TaxSummary
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.presentation.exception.GlobalExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.math.BigDecimal

@WebMvcTest(TaxSummaryController::class)
@Import(GlobalExceptionHandler::class)
class TaxSummaryControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockBean
    lateinit var taxSummaryQueryService: TaxSummaryQueryService

    @MockBean
    lateinit var taxSummaryCommandService: TaxSummaryCommandService

    @MockBean
    lateinit var taxSummaryBatchService: TaxSummaryBatchService

    @Test
    fun `단건 조회 성공 시 200과 요약을 반환한다`() {
        val summary = sampleSummary(accountId = 10L, taxYear = 2024)
        given(taxSummaryQueryService.get(10L, TaxYear(2024))).willReturn(summary)

        mockMvc.get("/api/accounts/10/tax-summaries/2024")
            .andExpect {
                status { isOk() }
                jsonPath("$.accountId") { value(10) }
                jsonPath("$.taxYear") { value(2024) }
            }
    }

    @Test
    fun `목록 조회 성공 시 200과 리스트를 반환한다`() {
        given(taxSummaryQueryService.list(10L, TaxYear(2023), TaxYear(2024))).willReturn(
            listOf(sampleSummary(accountId = 10L, taxYear = 2024), sampleSummary(accountId = 10L, taxYear = 2023))
        )

        mockMvc.get("/api/accounts/10/tax-summaries") {
            param("fromYear", "2023")
            param("toYear", "2024")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].taxYear") { value(2024) }
            jsonPath("$[1].taxYear") { value(2023) }
        }
    }

    @Test
    fun `재계산 성공 시 200을 반환한다`() {
        val summary = sampleSummary(accountId = 10L, taxYear = 2024)
        given(taxSummaryCommandService.recalculate(10L, TaxYear(2024), true)).willReturn(summary)

        mockMvc.post("/api/accounts/10/tax-summaries/2024/recalculate") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("force" to true))
        }.andExpect {
            status { isOk() }
            jsonPath("$.taxYear") { value(2024) }
        }
    }

    @Test
    fun `연말 배치 성공 시 200을 반환한다`() {
        given(taxSummaryBatchService.runYearEnd(TaxYear(2024), listOf(10L, 11L))).willReturn(2)

        mockMvc.post("/api/tax-summaries/jobs/year-end") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("taxYear" to 2024, "accountIds" to listOf(10, 11)))
        }.andExpect {
            status { isOk() }
            jsonPath("$.taxYear") { value(2024) }
            jsonPath("$.requestedAccounts") { value(2) }
        }
    }

    @Test
    fun `요약 미존재면 404 TAX_SUMMARY_NOT_FOUND`() {
        given(taxSummaryQueryService.get(10L, TaxYear(2024)))
            .willThrow(TaxSummaryNotFoundException("not found"))

        mockMvc.get("/api/accounts/10/tax-summaries/2024")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("TAX_SUMMARY_NOT_FOUND") }
            }
    }

    @Test
    fun `연도 범위 오류면 400 INVALID_TAX_YEAR_RANGE`() {
        given(taxSummaryQueryService.list(10L, TaxYear(2025), TaxYear(2024)))
            .willThrow(InvalidTaxYearRangeException("invalid"))

        mockMvc.get("/api/accounts/10/tax-summaries") {
            param("fromYear", "2025")
            param("toYear", "2024")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_TAX_YEAR_RANGE") }
        }
    }

    @Test
    fun `이미 실행 중이면 409 TAX_SUMMARY_ALREADY_RUNNING`() {
        given(taxSummaryCommandService.recalculate(10L, TaxYear(2024), false))
            .willThrow(TaxSummaryAlreadyRunningException("running"))

        mockMvc.post("/api/accounts/10/tax-summaries/2024/recalculate") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("force" to false))
        }.andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("TAX_SUMMARY_ALREADY_RUNNING") }
        }
    }

    @Test
    fun `지원하지 않는 통화면 422 UNSUPPORTED_CURRENCY`() {
        given(taxSummaryCommandService.recalculate(10L, TaxYear(2024), false))
            .willThrow(UnsupportedCurrencyException("unsupported"))

        mockMvc.post("/api/accounts/10/tax-summaries/2024/recalculate") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("force" to false))
        }.andExpect {
            status { isUnprocessableEntity() }
            jsonPath("$.code") { value("UNSUPPORTED_CURRENCY") }
        }
    }

    @Test
    fun `KIS 계좌 조회면 400 INVALID_ACCOUNT_MODE_FOR_TAX_SUMMARY`() {
        given(taxSummaryQueryService.get(10L, TaxYear(2024)))
            .willThrow(InvalidAccountModeForTaxSummaryException("LOCAL only"))

        mockMvc.get("/api/accounts/10/tax-summaries/2024")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("INVALID_ACCOUNT_MODE_FOR_TAX_SUMMARY") }
            }
    }

    @Test
    fun `KIS 계좌 재계산이면 400 INVALID_ACCOUNT_MODE_FOR_TAX_SUMMARY`() {
        given(taxSummaryCommandService.recalculate(10L, TaxYear(2024), false))
            .willThrow(InvalidAccountModeForTaxSummaryException("LOCAL only"))

        mockMvc.post("/api/accounts/10/tax-summaries/2024/recalculate") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("force" to false))
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_ACCOUNT_MODE_FOR_TAX_SUMMARY") }
        }
    }

    private fun sampleSummary(accountId: Long, taxYear: Int): TaxSummary {
        val account = Account.create(
            accountName = "test-account",
            accountType = AccountType.STOCK,
            tradingMode = TradingMode.LOCAL,
            initialDeposit = BigDecimal.ZERO,
        )
        setId(account, accountId)

        val summary = TaxSummary.create(
            account = account,
            taxYear = taxYear,
            totalRealizedPnl = BigDecimal("100.0000"),
            taxablePnl = BigDecimal("80.0000"),
            estimatedTax = BigDecimal("12.0000"),
        )
        setId(summary, 1L)
        return summary
    }

    private fun setId(target: Any, id: Long) {
        val field = target.javaClass.getDeclaredField("id")
        field.isAccessible = true
        field.set(target, id)
    }
}
