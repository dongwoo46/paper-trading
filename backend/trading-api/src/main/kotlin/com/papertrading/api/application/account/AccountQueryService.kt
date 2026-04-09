package com.papertrading.api.application.account

import com.papertrading.api.application.account.query.LedgerFilter
import com.papertrading.api.application.account.result.LedgerResult
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.model.Account
import com.papertrading.api.infrastructure.persistence.AccountLedgerRepository
import com.papertrading.api.infrastructure.persistence.AccountRepository
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AccountQueryService(
    private val accountRepository: AccountRepository,
    private val accountLedgerRepository: AccountLedgerRepository
) {

    fun getAccount(id: Long): Account =
        accountRepository.findById(id)
            .orElseThrow { NoSuchElementException("계좌를 찾을 수 없습니다. id=$id") }

    fun listAccounts(tradingMode: TradingMode?, isActive: Boolean?): List<Account> =
        when {
            tradingMode != null && isActive == true -> accountRepository.findByTradingModeAndIsActiveTrue(tradingMode)
            isActive == true -> accountRepository.findByIsActiveTrue()
            else -> accountRepository.findAll()
        }

    fun getLedgers(accountId: Long, filter: LedgerFilter): Page<LedgerResult> =
        accountLedgerRepository.findLedgers(accountId, filter)
}