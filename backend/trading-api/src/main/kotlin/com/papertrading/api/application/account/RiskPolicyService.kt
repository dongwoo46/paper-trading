package com.papertrading.api.application.account

import com.papertrading.api.application.account.command.UpsertRiskPolicyCommand
import com.papertrading.api.domain.model.RiskPolicy
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.RiskPolicyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RiskPolicyService(
    private val accountRepository: AccountRepository,
    private val riskPolicyRepository: RiskPolicyRepository
) {

    fun upsertRiskPolicy(accountId: Long, command: UpsertRiskPolicyCommand): RiskPolicy {
        val account = accountRepository.findByIdWithLock(accountId)
            .orElseThrow { NoSuchElementException("계좌를 찾을 수 없습니다. id=$accountId") }

        // 기존 활성 정책 비활성화 (dirty checking으로 자동 반영)
        riskPolicyRepository.findByAccountIdAndIsActiveTrue(accountId)
            .ifPresent { it.isActive = false }

        // Account Aggregate Root를 통해 새 정책 생성
        val newPolicy = account.createRiskPolicy(
            maxPositionRatio = command.maxPositionRatio,
            maxDailyLoss = command.maxDailyLoss,
            maxOrderAmount = command.maxOrderAmount
        )

        return riskPolicyRepository.save(newPolicy)
    }

    @Transactional(readOnly = true)
    fun getActiveRiskPolicy(accountId: Long): RiskPolicy =
        riskPolicyRepository.findByAccountIdAndIsActiveTrue(accountId)
            .orElseThrow { NoSuchElementException("활성 리스크 정책이 없습니다. accountId=$accountId") }
}
