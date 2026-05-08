package com.papertrading.api.application.account

import com.papertrading.api.application.account.command.CreateAccountCommand
import com.papertrading.api.application.account.command.DepositCommand
import com.papertrading.api.application.account.command.UpdateAccountCommand
import com.papertrading.api.application.account.command.WithdrawCommand
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.account.AccountLedger
import com.papertrading.api.common.exception.AccountNotFoundException
import com.papertrading.api.infrastructure.persistence.AccountLedgerRepository
import com.papertrading.api.infrastructure.persistence.AccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional
class AccountCommandService(
    private val accountRepository: AccountRepository,
    private val accountLedgerRepository: AccountLedgerRepository
) {

    fun createAccount(command: CreateAccountCommand): Account {
        val account = Account.create(
            accountName = command.accountName,
            accountType = command.accountType,
            tradingMode = command.tradingMode,
            initialDeposit = command.initialDeposit,
            baseCurrency = command.baseCurrency,
            externalAccountId = command.externalAccountId
        )
        val saved = accountRepository.save(account)

        if (command.initialDeposit > BigDecimal.ZERO) {
            accountLedgerRepository.save(saved.recordInitialDeposit("init-account-${saved.id}"))
        }
        return saved
    }

    fun deposit(accountId: Long, command: DepositCommand): AccountLedger {
        val account = accountRepository.findByIdWithLock(accountId)
            .orElseThrow { AccountNotFoundException(accountId) }

        accountLedgerRepository.findByIdempotencyKey(command.idempotencyKey)
            ?.let { return it }

        return accountLedgerRepository.save(
            account.recordDeposit(command.amount, command.idempotencyKey, command.description)
        )
    }

    fun withdraw(accountId: Long, command: WithdrawCommand): AccountLedger {
        val account = accountRepository.findByIdWithLock(accountId)
            .orElseThrow { AccountNotFoundException(accountId) }

        accountLedgerRepository.findByIdempotencyKey(command.idempotencyKey)
            ?.let { return it }

        return accountLedgerRepository.save(
            account.recordWithdrawal(command.amount, command.idempotencyKey, command.description)
        )
    }

    fun updateAccount(accountId: Long, command: UpdateAccountCommand): Account {
        val account = accountRepository.findById(accountId)
            .orElseThrow { AccountNotFoundException(accountId) }

        command.accountName?.let { account.rename(it) }
        command.externalAccountId?.let { account.updateExternalAccountId(it) }

        return account
    }

    fun deactivateAccount(accountId: Long) {
        val account = accountRepository.findById(accountId)
            .orElseThrow { AccountNotFoundException(accountId) }
        account.deactivate()
    }
}
