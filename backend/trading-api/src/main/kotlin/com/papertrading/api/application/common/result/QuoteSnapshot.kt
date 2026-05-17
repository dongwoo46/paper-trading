package com.papertrading.api.application.common.result

import com.papertrading.api.domain.enums.TradingMode
import java.math.BigDecimal
import java.time.Instant

data class QuoteSnapshot(
    val ticker: String,
    val tradingMode: TradingMode,
    val price: BigDecimal,
    val askp1: BigDecimal,
    val bidp1: BigDecimal,
    val updatedAt: Instant,
)
