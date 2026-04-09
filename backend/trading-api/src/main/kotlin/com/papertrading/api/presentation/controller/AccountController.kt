package com.papertrading.api.presentation.controller

import com.papertrading.api.application.account.AccountCommandService
import com.papertrading.api.application.account.AccountQueryService
import com.papertrading.api.application.account.command.CreateAccountCommand
import com.papertrading.api.application.account.command.DepositCommand
import com.papertrading.api.application.account.command.UpdateAccountCommand
import com.papertrading.api.application.account.command.WithdrawCommand
import com.papertrading.api.application.account.query.LedgerFilter
import com.papertrading.api.domain.enums.TransactionType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.presentation.dto.account.AccountResponse
import com.papertrading.api.presentation.dto.account.CreateAccountRequest
import com.papertrading.api.presentation.dto.account.DepositRequest
import com.papertrading.api.presentation.dto.account.LedgerCreatedResponse
import com.papertrading.api.presentation.dto.account.LedgerResponse
import com.papertrading.api.presentation.dto.account.PageResponse
import com.papertrading.api.presentation.dto.account.UpdateAccountRequest
import com.papertrading.api.presentation.dto.account.WithdrawRequest
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.ZoneOffset

@RestController
@RequestMapping("/api/v1/accounts")
class AccountController(
    private val accountCommandService: AccountCommandService,
    private val accountQueryService: AccountQueryService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createAccount(@Valid @RequestBody request: CreateAccountRequest): AccountResponse {
        val account = accountCommandService.createAccount(
            CreateAccountCommand(
                accountName = request.accountName,
                accountType = request.accountType,
                tradingMode = request.tradingMode,
                initialDeposit = request.initialDeposit,
                baseCurrency = request.baseCurrency,
                externalAccountId = request.externalAccountId
            )
        )
        return AccountResponse.from(account)
    }

    @GetMapping
    fun listAccounts(
        @RequestParam(required = false) tradingMode: TradingMode?,
        @RequestParam(required = false) isActive: Boolean?
    ): List<AccountResponse> =
        accountQueryService.listAccounts(tradingMode, isActive).map { AccountResponse.from(it) }

    @GetMapping("/{id}")
    fun getAccount(@PathVariable id: Long): AccountResponse =
        AccountResponse.from(accountQueryService.getAccount(id))

    @PatchMapping("/{id}")
    fun updateAccount(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateAccountRequest
    ): AccountResponse {
        val account = accountCommandService.updateAccount(
            id,
            UpdateAccountCommand(
                accountName = request.accountName,
                externalAccountId = request.externalAccountId
            )
        )
        return AccountResponse.from(account)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deactivateAccount(@PathVariable id: Long) =
        accountCommandService.deactivateAccount(id)

    @PostMapping("/{id}/deposit")
    fun deposit(
        @PathVariable id: Long,
        @Valid @RequestBody request: DepositRequest
    ): LedgerCreatedResponse {
        val ledger = accountCommandService.deposit(
            id,
            DepositCommand(
                amount = request.amount,
                idempotencyKey = request.idempotencyKey,
                description = request.description
            )
        )
        return LedgerCreatedResponse(
            ledgerId = ledger.id!!,
            balanceAfter = ledger.balanceAfter,
            transactionType = ledger.transactionType!!
        )
    }

    @PostMapping("/{id}/withdraw")
    fun withdraw(
        @PathVariable id: Long,
        @Valid @RequestBody request: WithdrawRequest
    ): LedgerCreatedResponse {
        val ledger = accountCommandService.withdraw(
            id,
            WithdrawCommand(
                amount = request.amount,
                idempotencyKey = request.idempotencyKey,
                description = request.description
            )
        )
        return LedgerCreatedResponse(
            ledgerId = ledger.id!!,
            balanceAfter = ledger.balanceAfter,
            transactionType = ledger.transactionType!!
        )
    }

    @GetMapping("/{id}/ledgers")
    fun getLedgers(
        @PathVariable id: Long,
        @RequestParam(required = false) type: TransactionType?,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageResponse<LedgerResponse> {
        val filter = LedgerFilter(
            transactionType = type,
            from = from?.atStartOfDay(ZoneOffset.UTC)?.toInstant(),
            to = to?.plusDays(1)?.atStartOfDay(ZoneOffset.UTC)?.toInstant(),
            pageable = PageRequest.of(page, size.coerceIn(1, 100))
        )
        val result = accountQueryService.getLedgers(id, filter)
        return PageResponse(
            content = result.content.map { LedgerResponse.from(it) },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }
}
