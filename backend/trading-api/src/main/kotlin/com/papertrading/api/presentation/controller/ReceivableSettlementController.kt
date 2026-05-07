package com.papertrading.api.presentation.controller

import com.papertrading.api.application.account.ReceivableSettlementQueryService
import com.papertrading.api.application.account.query.ReceivableSettlementFilter
import com.papertrading.api.domain.enums.SettlementStatus
import com.papertrading.api.presentation.dto.account.ReceivableSettlementResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/receivable-settlements")
class ReceivableSettlementController(
    private val ReceivableSettlementQueryService: ReceivableSettlementQueryService
) {

    @GetMapping
    fun listReceivableSettlements(
        @PathVariable accountId: Long,
        @RequestParam(required = false) status: SettlementStatus?,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?
    ): List<ReceivableSettlementResponse> {
        val filter = ReceivableSettlementFilter(status = status, from = from, to = to)
        return ReceivableSettlementQueryService.listReceivableSettlements(accountId, filter)
            .map { ReceivableSettlementResponse.from(it) }
    }
}
