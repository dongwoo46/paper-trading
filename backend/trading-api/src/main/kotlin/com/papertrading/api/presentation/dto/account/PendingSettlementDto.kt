package com.papertrading.api.presentation.dto.account

import com.papertrading.api.application.account.result.PendingSettlementResult
import com.papertrading.api.domain.enums.SettlementStatus
import java.math.BigDecimal
import java.time.LocalDate

data class PendingSettlementResponse(
    val id: Long,
    val orderId: Long,
    val settlementDate: LocalDate,
    val amount: BigDecimal,
    val status: SettlementStatus
) {
    companion object {
        fun from(result: PendingSettlementResult) = PendingSettlementResponse(
            id = result.id,
            orderId = result.orderId,
            settlementDate = result.settlementDate,
            amount = result.amount,
            status = result.status
        )
    }
}