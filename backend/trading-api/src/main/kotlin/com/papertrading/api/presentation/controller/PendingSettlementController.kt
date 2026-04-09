package com.papertrading.api.presentation.controller

import com.papertrading.api.application.account.PendingSettlementQueryService
import com.papertrading.api.application.account.query.PendingSettlementFilter
import com.papertrading.api.domain.enums.SettlementStatus
import com.papertrading.api.presentation.dto.account.PendingSettlementResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/pending-settlements")
class PendingSettlementController(
    private val pendingSettlementQueryService: PendingSettlementQueryService
) {

    @GetMapping
    fun listPendingSettlements(
        @PathVariable accountId: Long,
        @RequestParam(required = false) status: SettlementStatus?,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?
    ): List<PendingSettlementResponse> {
        val filter = PendingSettlementFilter(status = status, from = from, to = to)
        return pendingSettlementQueryService.listPendingSettlements(accountId, filter)
            .map { PendingSettlementResponse.from(it) }
    }
}