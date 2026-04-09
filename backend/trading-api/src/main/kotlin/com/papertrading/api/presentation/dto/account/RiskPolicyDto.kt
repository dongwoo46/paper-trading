package com.papertrading.api.presentation.dto.account

import com.papertrading.api.domain.model.RiskPolicy
import java.math.BigDecimal

data class UpsertRiskPolicyRequest(
    val maxPositionRatio: BigDecimal?,
    val maxDailyLoss: BigDecimal?,
    val maxOrderAmount: BigDecimal?
)

data class RiskPolicyResponse(
    val id: Long,
    val maxPositionRatio: BigDecimal?,
    val maxDailyLoss: BigDecimal?,
    val maxOrderAmount: BigDecimal?,
    val isActive: Boolean
) {
    companion object {
        fun from(policy: RiskPolicy) = RiskPolicyResponse(
            id = policy.id!!,
            maxPositionRatio = policy.maxPositionRatio,
            maxDailyLoss = policy.maxDailyLoss,
            maxOrderAmount = policy.maxOrderAmount,
            isActive = policy.isActive
        )
    }
}
