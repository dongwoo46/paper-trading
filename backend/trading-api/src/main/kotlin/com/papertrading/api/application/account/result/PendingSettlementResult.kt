package com.papertrading.api.application.account.result

import com.papertrading.api.domain.enums.SettlementStatus
import java.math.BigDecimal
import java.time.LocalDate

data class PendingSettlementResult(
    val id: Long,
    val orderId: Long,
    val settlementDate: LocalDate,
    val amount: BigDecimal,
    val status: SettlementStatus
)
