package com.papertrading.api.presentation.controller

import com.papertrading.api.application.toss.TossAccountListQueryService
import com.papertrading.api.presentation.dto.toss.TossAccountResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/toss")
class TossAccountController(
    private val tossAccountListQueryService: TossAccountListQueryService,
) {
    @GetMapping("/accounts")
    fun listAccounts(): List<TossAccountResponse> {
        val accounts = tossAccountListQueryService.listAccounts()
        return accounts.map(TossAccountResponse::from)
    }
}
