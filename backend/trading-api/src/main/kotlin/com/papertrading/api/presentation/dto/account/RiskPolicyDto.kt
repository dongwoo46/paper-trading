package com.papertrading.api.presentation.dto.account

import com.papertrading.api.domain.entity.account.RiskPolicy
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import java.math.BigDecimal

data class UpsertRiskPolicyRequest(
    @field:DecimalMin("0.0")
    @field:DecimalMax("1.0")
    val maxPositionRatio: BigDecimal?,
    @field:DecimalMin("0.00000001")
    val maxDailyLoss: BigDecimal?,
    @field:DecimalMin("0.00000001")
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
