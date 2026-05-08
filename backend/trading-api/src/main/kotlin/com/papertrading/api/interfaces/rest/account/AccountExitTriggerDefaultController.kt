package com.papertrading.api.interfaces.rest.account

import com.papertrading.api.application.account.AccountExitTriggerDefaultCommandService
import com.papertrading.api.interfaces.rest.account.dto.UpsertAccountExitTriggerDefaultRequest
import com.papertrading.api.interfaces.rest.account.dto.toResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/accounts")
class AccountExitTriggerDefaultController(
    private val commandService: AccountExitTriggerDefaultCommandService,
) {
    @PutMapping("/{accountId}/exit-trigger-default")
    fun upsert(
        @PathVariable accountId: Long,
        @Valid @RequestBody request: UpsertAccountExitTriggerDefaultRequest,
    ) = commandService.upsert(request.toCommand(accountId)).toResponse()
}
