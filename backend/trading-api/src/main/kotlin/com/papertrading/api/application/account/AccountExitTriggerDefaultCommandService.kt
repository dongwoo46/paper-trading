package com.papertrading.api.application.account

import com.papertrading.api.application.account.command.UpsertAccountExitTriggerDefaultCommand
import com.papertrading.api.application.account.result.AccountExitTriggerDefaultResult
import com.papertrading.api.common.exception.AccountNotFoundException
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.AccountExitTriggerDefaultRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant



@Service
class AccountExitTriggerDefaultCommandService(
    private val accountRepository: AccountRepository,
    private val accountExitTriggerDefaultRepository: AccountExitTriggerDefaultRepository,
) {
    @Transactional
    fun upsert(command: UpsertAccountExitTriggerDefaultCommand): AccountExitTriggerDefaultResult {
        val account = accountRepository.findById(command.accountId)
            .orElseThrow { AccountNotFoundException(command.accountId) }
        val current = accountExitTriggerDefaultRepository.findByAccountId(command.accountId)
        val entity = if (current == null) {
            account.createExitTriggerDefault(
                enabled = command.enabled,
                stopLossPercent = command.stopLossPercent,
                takeProfitPercent = command.takeProfitPercent,
            )
        } else {
            account.updateExitTriggerDefault(
                existing = current,
                enabled = command.enabled,
                stopLossPercent = command.stopLossPercent,
                takeProfitPercent = command.takeProfitPercent,
            )
        }
        val saved = accountExitTriggerDefaultRepository.save(entity)
        return AccountExitTriggerDefaultResult(
            accountId = saved.accountId,
            enabled = saved.enabled,
            stopLossPercent = saved.stopLossPercent,
            takeProfitPercent = saved.takeProfitPercent,
            updatedAt = saved.updatedAt,
        )
    }
}
