package com.papertrading.api.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.api.application.portfolio.DailyBalanceQueryService
import com.papertrading.api.application.portfolio.InvalidDateRangeException
import com.papertrading.api.application.portfolio.SnapshotAlreadyRunningException
import com.papertrading.api.application.portfolio.SnapshotComputeFailedException
import com.papertrading.api.application.portfolio.SnapshotJobService
import com.papertrading.api.application.portfolio.PortfolioSnapshotQueryService
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.portfolio.DailyBalance
import com.papertrading.api.domain.entity.portfolio.PortfolioSnapshot
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.presentation.exception.GlobalExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.LocalDate

@WebMvcTest(PortfolioSnapshotController::class)
@Import(GlobalExceptionHandler::class)
class PortfolioSnapshotControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockBean
    lateinit var dailyBalanceQueryService: DailyBalanceQueryService

    @MockBean
    lateinit var portfolioSnapshotQueryService: PortfolioSnapshotQueryService

    @MockBean
    lateinit var snapshotJobService: SnapshotJobService

    @Test
    fun `daily balances 조회 성공 시 200`() {
        val account = sampleAccount(10L)
        val balance = DailyBalance.create(
            account = account,
            businessDate = LocalDate.of(2026, 5, 1),
            cashBalance = BigDecimal("1000000"),
            stockMarketValue = BigDecimal("200000"),
            totalAssetValue = BigDecimal("1200000"),
            pnlAmount = BigDecimal("200000"),
            pnlRate = BigDecimal("0.200000"),
        )
        given(dailyBalanceQueryService.getDailyBalances(10L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 2)))
            .willReturn(listOf(balance))

        mockMvc.get("/api/accounts/10/daily-balances") {
            param("fromDate", "2026-05-01")
            param("toDate", "2026-05-02")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].businessDate") { value("2026-05-01") }
            jsonPath("$[0].pnlRate") { value(0.2) }
        }
    }

    @Test
    fun `portfolio snapshots 조회 성공 시 200`() {
        val account = sampleAccount(10L)
        val snapshot = PortfolioSnapshot.create(
            account = account,
            businessDate = LocalDate.of(2026, 5, 1),
            ticker = "005930",
            quantity = BigDecimal("10"),
            avgBuyPrice = BigDecimal("70000"),
            closePrice = BigDecimal("72000"),
            marketValue = BigDecimal("720000"),
            weight = BigDecimal("0.600000"),
            unrealizedPnl = BigDecimal("20000"),
        )
        given(portfolioSnapshotQueryService.getPortfolioSnapshots(10L, LocalDate.of(2026, 5, 1)))
            .willReturn(listOf(snapshot))

        mockMvc.get("/api/accounts/10/portfolio-snapshots") {
            param("date", "2026-05-01")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].ticker") { value("005930") }
            jsonPath("$[0].weight") { value(0.6) }
        }
    }

    @Test
    fun `date range 오류는 400 INVALID_DATE_RANGE`() {
        given(dailyBalanceQueryService.getDailyBalances(10L, LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 1)))
            .willThrow(InvalidDateRangeException("invalid"))

        mockMvc.get("/api/accounts/10/daily-balances") {
            param("fromDate", "2026-05-02")
            param("toDate", "2026-05-01")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_DATE_RANGE") }
        }
    }

    @Test
    fun `businessDate 파싱 오류는 400 INVALID_BUSINESS_DATE`() {
        mockMvc.get("/api/accounts/10/portfolio-snapshots") {
            param("date", "20260501")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_BUSINESS_DATE") }
        }
    }

    @Test
    fun `daily job 실행 성공 시 200`() {
        given(snapshotJobService.generateDailySnapshots(10L, LocalDate.of(2026, 5, 1))).willReturn(1)

        mockMvc.post("/api/accounts/10/portfolio-snapshots/jobs/daily") {
            param("businessDate", "2026-05-01")
        }.andExpect {
            status { isOk() }
            jsonPath("$.accountId") { value(10) }
            jsonPath("$.snapshotCount") { value(1) }
        }
    }

    @Test
    fun `daily job conflict 는 409 SNAPSHOT_ALREADY_RUNNING`() {
        given(snapshotJobService.generateDailySnapshots(10L, LocalDate.of(2026, 5, 1)))
            .willThrow(SnapshotAlreadyRunningException("running"))

        mockMvc.post("/api/accounts/10/portfolio-snapshots/jobs/daily") {
            param("businessDate", "2026-05-01")
        }.andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("SNAPSHOT_ALREADY_RUNNING") }
        }
    }

    @Test
    fun `daily job failure 는 500 SNAPSHOT_COMPUTE_FAILED`() {
        given(snapshotJobService.generateDailySnapshots(10L, LocalDate.of(2026, 5, 1)))
            .willThrow(SnapshotComputeFailedException("failed"))

        mockMvc.post("/api/accounts/10/portfolio-snapshots/jobs/daily") {
            param("businessDate", "2026-05-01")
        }.andExpect {
            status { isInternalServerError() }
            jsonPath("$.code") { value("SNAPSHOT_COMPUTE_FAILED") }
        }
    }

    private fun sampleAccount(id: Long): Account {
        val account = Account.create(
            accountName = "test-account",
            accountType = AccountType.STOCK,
            tradingMode = TradingMode.LOCAL,
            initialDeposit = BigDecimal.ZERO,
        )
        val field = account.javaClass.getDeclaredField("id")
        field.isAccessible = true
        field.set(account, id)
        return account
    }
}