package com.papertrading.api.application.account.result

import com.papertrading.api.domain.enums.TransactionType
import java.math.BigDecimal
import java.time.Instant

data class LedgerResult(
    val id: Long,
    val transactionType: TransactionType,
    val amount: BigDecimal,
    val balanceAfter: BigDecimal,
    val refOrderId: Long?,
    val refExecutionId: Long?,
    val description: String?,
    val createdAt: Instant?
)
