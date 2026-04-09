package com.papertrading.api.presentation.controller

import com.papertrading.api.application.account.RiskPolicyService
import com.papertrading.api.application.account.command.UpsertRiskPolicyCommand
import com.papertrading.api.presentation.dto.account.RiskPolicyResponse
import com.papertrading.api.presentation.dto.account.UpsertRiskPolicyRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/risk-policy")
class RiskPolicyController(
    private val riskPolicyService: RiskPolicyService
) {

    @GetMapping
    fun getRiskPolicy(@PathVariable accountId: Long): RiskPolicyResponse =
        RiskPolicyResponse.from(riskPolicyService.getActiveRiskPolicy(accountId))

    @PostMapping
    fun upsertRiskPolicy(
        @PathVariable accountId: Long,
        @RequestBody request: UpsertRiskPolicyRequest
    ): RiskPolicyResponse {
        val policy = riskPolicyService.upsertRiskPolicy(
            accountId,
            UpsertRiskPolicyCommand(
                maxPositionRatio = request.maxPositionRatio,
                maxDailyLoss = request.maxDailyLoss,
                maxOrderAmount = request.maxOrderAmount
            )
        )
        return RiskPolicyResponse.from(policy)
    }
}
