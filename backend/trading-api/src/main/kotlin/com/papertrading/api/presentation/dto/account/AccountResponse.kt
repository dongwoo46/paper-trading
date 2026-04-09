package com.papertrading.api.presentation.dto.account

import com.papertrading.api.application.account.result.LedgerResult
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TransactionType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.model.Account
import com.papertrading.api.domain.model.AccountLedger
import java.math.BigDecimal
import java.time.Instant

data class AccountResponse(
    val id: Long,
    val accountName: String,
    val accountType: AccountType,
    val tradingMode: TradingMode,
    val deposit: BigDecimal,
    val availableDeposit: BigDecimal,
    val lockedDeposit: BigDecimal,
    val baseCurrency: String,
    val externalAccountId: String?,
    val isActive: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?
) {
    companion object {
        fun from(account: Account) = AccountResponse(
            id = account.id!!,
            accountName = account.accountName!!,
            accountType = account.accountType!!,
            tradingMode = account.tradingMode!!,
            deposit = account.deposit,
            availableDeposit = account.availableDeposit,
            lockedDeposit = account.lockedDeposit,
            baseCurrency = account.baseCurrency,
            externalAccountId = account.externalAccountId,
            isActive = account.isActive,
            createdAt = account.createdAt,
            updatedAt = account.updatedAt
        )
    }
}

data class LedgerResponse(
    val id: Long,
    val transactionType: TransactionType,
    val amount: BigDecimal,
    val balanceAfter: BigDecimal,
    val refOrderId: Long?,
    val refExecutionId: Long?,
    val description: String?,
    val createdAt: Instant?
) {
    companion object {
        fun from(ledger: AccountLedger) = LedgerResponse(
            id = ledger.id!!,
            transactionType = ledger.transactionType!!,
            amount = ledger.amount,
            balanceAfter = ledger.balanceAfter,
            refOrderId = ledger.refOrderId,
            refExecutionId = ledger.refExecutionId,
            description = ledger.description,
            createdAt = ledger.createdAt
        )

        fun from(result: LedgerResult) = LedgerResponse(
            id = result.id,
            transactionType = result.transactionType,
            amount = result.amount,
            balanceAfter = result.balanceAfter,
            refOrderId = result.refOrderId,
            refExecutionId = result.refExecutionId,
            description = result.description,
            createdAt = result.createdAt
        )
    }
}

data class LedgerCreatedResponse(
    val ledgerId: Long,
    val balanceAfter: BigDecimal,
    val transactionType: TransactionType
)

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)