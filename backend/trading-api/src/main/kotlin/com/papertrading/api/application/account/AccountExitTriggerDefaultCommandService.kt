package com.papertrading.api.application.account

import com.papertrading.api.common.exception.AccountNotFoundException
import com.papertrading.api.common.exception.BadRequestException
import com.papertrading.api.domain.account.AccountExitTriggerDefault
import com.papertrading.api.domain.account.AccountExitTriggerDefaultRepository
import com.papertrading.api.infrastructure.persistence.AccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

data class UpsertAccountExitTriggerDefaultCommand(
    val accountId: Long,
    val enabled: Boolean,
    val stopLossPercent: BigDecimal?,
    val takeProfitPercent: BigDecimal?,
)

data class AccountExitTriggerDefaultResult(
    val accountId: Long,
    val enabled: Boolean,
    val stopLossPercent: BigDecimal?,
    val takeProfitPercent: BigDecimal?,
    val updatedAt: Instant?,
)

@Service
class AccountExitTriggerDefaultCommandService(
    private val accountRepository: AccountRepository,
    private val accountExitTriggerDefaultRepository: AccountExitTriggerDefaultRepository,
) {
    @Transactional
    fun upsert(command: UpsertAccountExitTriggerDefaultCommand): AccountExitTriggerDefaultResult {
        accountRepository.findById(command.accountId).orElseThrow { AccountNotFoundException(command.accountId) }
        validatePercentScale(command.stopLossPercent)
        validatePercentScale(command.takeProfitPercent)
        val current = accountExitTriggerDefaultRepository.findByAccountId(command.accountId)
        val entity = if (current == null) {
            AccountExitTriggerDefault.create(
                accountId = command.accountId,
                enabled = command.enabled,
                stopLossPercent = command.stopLossPercent,
                takeProfitPercent = command.takeProfitPercent,
            )
        } else {
            current.enabled = command.enabled
            current.stopLossPercent = command.stopLossPercent
            current.takeProfitPercent = command.takeProfitPercent
            current.validate()
            current
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

    private fun validatePercentScale(value: BigDecimal?) {
        if (value != null && value.stripTrailingZeros().scale() > 4) {
            throw BadRequestException("INVALID_PERCENT_SCALE", "percent scale must be <= 4")
        }
    }
}
