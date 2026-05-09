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
class AccountCommandService(
    private val accountRepository: AccountRepository,
    private val accountLedgerRepository: AccountLedgerRepository
) {

    // 계좌 생성
    @Transactional
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
        // 원장 생성
        if (command.initialDeposit > BigDecimal.ZERO) {
            accountLedgerRepository.save(saved.recordInitialDeposit("init-account-${saved.id}"))
        }
        return saved
    }

    @Transactional
    fun deposit(accountId: Long, command: DepositCommand): AccountLedger {
        val account = accountRepository.findByIdWithLock(accountId)
            .orElseThrow { AccountNotFoundException(accountId) }

        // 멱등성을 위해 멱등키
        accountLedgerRepository.findByAccountIdAndIdempotencyKey(
            accountId = accountId,
            idempotencyKey = command.idempotencyKey,
        )?.let { return it }

        return accountLedgerRepository.save(
            account.recordDeposit(command.amount, command.idempotencyKey, command.description)
        )
    }

    @Transactional
    fun withdraw(accountId: Long, command: WithdrawCommand): AccountLedger {
        val account = accountRepository.findByIdWithLock(accountId)
            .orElseThrow { AccountNotFoundException(accountId) }

        accountLedgerRepository.findByAccountIdAndIdempotencyKey(
            accountId = accountId,
            idempotencyKey = command.idempotencyKey,
        )?.let { return it }

        return accountLedgerRepository.save(
            account.recordWithdrawal(command.amount, command.idempotencyKey, command.description)
        )
    }

    // 돈과 관련된 것이 아니기 떄문에 락X
    @Transactional
    fun updateAccount(accountId: Long, command: UpdateAccountCommand): Account {
        val account = accountRepository.findById(accountId)
            .orElseThrow { AccountNotFoundException(accountId) }

        command.accountName?.let { account.rename(it) }
        command.externalAccountId?.let { account.updateExternalAccountId(it) }

        return account
    }

    @Transactional
    fun deactivateAccount(accountId: Long) {
        val account = accountRepository.findById(accountId)
            .orElseThrow { AccountNotFoundException(accountId) }
        account.deactivate()
    }
}
