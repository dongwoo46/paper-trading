package com.papertrading.api.presentation.controller

import com.papertrading.api.application.portfolio.TradingJournalCommandService
import com.papertrading.api.application.portfolio.TradingJournalQueryService
import com.papertrading.api.application.portfolio.command.CreateTradingJournalCommand
import com.papertrading.api.application.portfolio.command.UpdateTradingJournalCommand
import com.papertrading.api.application.portfolio.query.TradingJournalFilter
import com.papertrading.api.presentation.dto.portfolio.TradingJournalCreateRequest
import com.papertrading.api.presentation.dto.portfolio.TradingJournalListResponse
import com.papertrading.api.presentation.dto.portfolio.TradingJournalResponse
import com.papertrading.api.presentation.dto.portfolio.TradingJournalUpdateRequest
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/trading-journals")
class TradingJournalController(
    private val tradingJournalCommandService: TradingJournalCommandService,
    private val tradingJournalQueryService: TradingJournalQueryService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: TradingJournalCreateRequest
    ): TradingJournalResponse = TradingJournalResponse.from(
        tradingJournalCommandService.create(
            CreateTradingJournalCommand(
                accountId = request.accountId,
                journalType = request.journalType,
                title = request.title,
                content = request.content,
                orderId = request.orderId,
                ticker = request.ticker,
                sentiment = request.sentiment
            )
        )
    )

    @PatchMapping("/{journalId}")
    fun update(
        @PathVariable journalId: Long,
        @Valid @RequestBody request: TradingJournalUpdateRequest
    ): TradingJournalResponse = TradingJournalResponse.from(
        tradingJournalCommandService.update(
            journalId,
            UpdateTradingJournalCommand(
                accountId = request.accountId,
                title = request.title,
                content = request.content,
                sentiment = request.sentiment
            )
        )
    )

    @GetMapping
    fun list(
        @RequestParam accountId: Long,
        @RequestParam(required = false) ticker: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): TradingJournalListResponse {
        require(page >= 0) { "page must be greater than or equal to 0" }
        require(size in 1..100) { "size must be between 1 and 100" }

        val result = tradingJournalQueryService.list(
            TradingJournalFilter(accountId, ticker),
            PageRequest.of(
                page,
                size,
                Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
                )
            )
        ).map { TradingJournalResponse.from(it) }
        return TradingJournalListResponse.from(result)
    }

    @GetMapping("/{journalId}")
    fun get(
        @PathVariable journalId: Long,
        @RequestParam accountId: Long
    ): TradingJournalResponse = TradingJournalResponse.from(
        tradingJournalQueryService.get(journalId, accountId)
    )
}
