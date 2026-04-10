package com.papertrading.api.domain.port

import java.math.BigDecimal
import java.time.Instant

data class QuoteSnapshot(
    val ticker: String,
    val price: BigDecimal,    // H0STCNT0 체결가
    val askp1: BigDecimal,    // 매도호가1 (지정가 매수 조건 판단)
    val bidp1: BigDecimal,    // 매수호가1 (지정가 매도 조건 판단)
    val updatedAt: Instant,
)