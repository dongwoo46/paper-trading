package com.papertrading.api.presentation.controller

import com.papertrading.api.application.account.KisAccountQueryService
import com.papertrading.api.application.account.kis.KisAccountBalanceResult
import com.papertrading.api.application.account.kis.KisAccountMode
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/kis/account")
class KisAccountBalanceController(
    private val kisAccountQueryService: KisAccountQueryService,
) {
    @GetMapping("/balance")
    fun getBalance(
        @RequestParam accountId: Long,
        @RequestParam mode: KisAccountMode,
    ): KisAccountBalanceResult = kisAccountQueryService.getBalance(accountId, mode)
}
