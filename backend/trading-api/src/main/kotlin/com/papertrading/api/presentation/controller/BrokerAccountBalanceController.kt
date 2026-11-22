package com.papertrading.api.presentation.controller

import com.papertrading.api.application.account.BrokerAccountQueryService
import com.papertrading.api.presentation.dto.account.BrokerAccountBalanceResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/accounts")
class BrokerAccountBalanceController(
    private val brokerAccountQueryService: BrokerAccountQueryService,
) {
    @GetMapping("/{accountId}/broker-balance")
    fun getBrokerBalance(@PathVariable accountId: Long): BrokerAccountBalanceResponse {
        val result = brokerAccountQueryService.getBalance(accountId)
        return BrokerAccountBalanceResponse.from(result)
    }
}
