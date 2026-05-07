package com.papertrading.api.presentation.dto.account

import com.papertrading.api.application.account.result.ReceivableSettlementResult
import com.papertrading.api.domain.enums.SettlementStatus
import java.math.BigDecimal
import java.time.LocalDate

data class ReceivableSettlementResponse(
    val id: Long,
    val orderId: Long,
    val settlementDate: LocalDate,
    val amount: BigDecimal,
    val status: SettlementStatus
) {
    companion object {
        fun from(result: ReceivableSettlementResult) = ReceivableSettlementResponse(
            id = result.id,
            orderId = result.orderId,
            settlementDate = result.settlementDate,
            amount = result.amount,
            status = result.status
        )
    }
}
