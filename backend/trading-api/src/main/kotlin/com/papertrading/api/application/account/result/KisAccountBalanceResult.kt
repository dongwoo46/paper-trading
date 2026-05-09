package com.papertrading.api.application.account.result

import com.papertrading.api.domain.enums.AccountSource
import com.papertrading.api.domain.enums.KisAccountMode
import java.math.BigDecimal
import java.time.OffsetDateTime

data class KisAccountBalanceResult(
    val accountId: Long,
    val source: AccountSource = AccountSource.KIS,
    val mode: KisAccountMode,
    val asOf: OffsetDateTime,
    val cashBalance: BigDecimal,
    val marketValue: BigDecimal,
    val unrealizedPnl: BigDecimal,
    val returnRate: BigDecimal,
    val positions: List<KisBalancePositionResult>,
    val reconciliation: KisReconciliationResult
)
