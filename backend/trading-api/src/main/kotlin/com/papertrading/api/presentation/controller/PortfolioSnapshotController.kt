package com.papertrading.api.presentation.controller

import com.papertrading.api.application.portfolio.DailyBalanceQueryService
import com.papertrading.api.application.portfolio.InvalidBusinessDateException
import com.papertrading.api.application.portfolio.PortfolioSnapshotQueryService
import com.papertrading.api.application.portfolio.SnapshotJobService
import com.papertrading.api.presentation.dto.portfolio.DailyBalanceResponse
import com.papertrading.api.presentation.dto.portfolio.PortfolioSnapshotJobResponse
import com.papertrading.api.presentation.dto.portfolio.PortfolioSnapshotResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeParseException

@RestController
@RequestMapping("/api/accounts/{accountId}")
class PortfolioSnapshotController(
    private val dailyBalanceQueryService: DailyBalanceQueryService,
    private val portfolioSnapshotQueryService: PortfolioSnapshotQueryService,
    private val snapshotJobService: SnapshotJobService,
) {
    @GetMapping("/daily-balances")
    fun getDailyBalances(
        @PathVariable accountId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate,
    ): List<DailyBalanceResponse> = dailyBalanceQueryService.getDailyBalances(accountId, fromDate, toDate)
        .map { DailyBalanceResponse.from(it) }

    @GetMapping("/portfolio-snapshots")
    fun getPortfolioSnapshots(
        @PathVariable accountId: Long,
        @RequestParam("date") date: String,
    ): List<PortfolioSnapshotResponse> {
        val businessDate = parseBusinessDate(date)
        return portfolioSnapshotQueryService.getPortfolioSnapshots(accountId, businessDate)
            .map { PortfolioSnapshotResponse.from(it) }
    }

    @PostMapping("/portfolio-snapshots/jobs/daily")
    fun generateDailySnapshots(
        @PathVariable accountId: Long,
        @RequestParam("businessDate") businessDateRaw: String,
    ): PortfolioSnapshotJobResponse {
        val businessDate = parseBusinessDate(businessDateRaw)
        val snapshotCount = snapshotJobService.generateDailySnapshots(accountId, businessDate)
        return PortfolioSnapshotJobResponse(
            accountId = accountId,
            businessDate = businessDate,
            snapshotCount = snapshotCount,
        )
    }

    private fun parseBusinessDate(raw: String): LocalDate = try {
        LocalDate.parse(raw)
    } catch (_: DateTimeParseException) {
        throw InvalidBusinessDateException("businessDate 형식이 올바르지 않습니다. value=$raw")
    }
}