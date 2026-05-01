package com.papertrading.api.infrastructure.kis

import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.TradingMode
import java.math.BigDecimal
import java.time.Instant

data class KisExecutionNotice(
    val mode: TradingMode,
    val channelId: String,
    val externalOrderId: String,
    val externalExecutionId: String,
    val ticker: String,
    val side: OrderSide,
    val executedQty: BigDecimal,
    val executedPrice: BigDecimal,
    val executedAt: Instant,
    val accountNumber: String? = null,
    val accountProductCode: String? = null,
) {
    val accountScope: String?
        get() = if (!accountNumber.isNullOrBlank() && !accountProductCode.isNullOrBlank()) {
            "$accountNumber-$accountProductCode"
        } else {
            null
        }
}
