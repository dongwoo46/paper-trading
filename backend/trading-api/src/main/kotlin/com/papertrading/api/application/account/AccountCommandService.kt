package com.papertrading.api.application.account

import com.papertrading.api.application.account.command.CreateAccountCommand
import com.papertrading.api.application.account.command.DepositCommand
import com.papertrading.api.application.account.command.UpdateAccountCommand
import com.papertrading.api.application.account.command.WithdrawCommand
import com.papertrading.api.domain.enums.TransactionType
import com.papertrading.api.domain.model.Account
import com.papertrading.api.domain.model.AccountLedger
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
            accountLedgerRepository.save(
                AccountLedger(
                    account = saved,
                    transactionType = TransactionType.DEPOSIT,
                    amount = command.initialDeposit,
                    balanceAfter = saved.availableDeposit,
                    idempotencyKey = "init-account-${saved.id}"
                )
            )
        }
        return saved
    }

    fun deposit(accountId: Long, command: DepositCommand): AccountLedger {
        val account = accountRepository.findByIdWithLock(accountId)
            .orElseThrow { NoSuchElementException("계좌를 찾을 수 없습니다. id=$accountId") }

        accountLedgerRepository.findByIdempotencyKey(command.idempotencyKey)
            ?.let { return it }

        account.deposit(command.amount)

        return accountLedgerRepository.save(
            AccountLedger(
                account = account,
                transactionType = TransactionType.DEPOSIT,
                amount = command.amount,
                balanceAfter = account.availableDeposit,
                description = command.description,
                idempotencyKey = command.idempotencyKey
            )
        )
    }

    fun withdraw(accountId: Long, command: WithdrawCommand): AccountLedger {
        val account = accountRepository.findByIdWithLock(accountId)
            .orElseThrow { NoSuchElementException("계좌를 찾을 수 없습니다. id=$accountId") }

        accountLedgerRepository.findByIdempotencyKey(command.idempotencyKey)
            ?.let { return it }

        account.withdraw(command.amount)

        return accountLedgerRepository.save(
            AccountLedger(
                account = account,
                transactionType = TransactionType.WITHDRAWAL,
                amount = command.amount,
                balanceAfter = account.availableDeposit,
                description = command.description,
                idempotencyKey = command.idempotencyKey
            )
        )
    }

    fun updateAccount(accountId: Long, command: UpdateAccountCommand): Account {
        val account = accountRepository.findById(accountId)
            .orElseThrow { NoSuchElementException("계좌를 찾을 수 없습니다. id=$accountId") }

        command.accountName?.let { account.rename(it) }
        command.externalAccountId?.let { account.updateExternalAccountId(it) }

        return account
    }

    fun deactivateAccount(accountId: Long) {
        val account = accountRepository.findById(accountId)
            .orElseThrow { NoSuchElementException("계좌를 찾을 수 없습니다. id=$accountId") }
        account.deactivate()
    }
}