package com.papertrading.collector.domain.kis

import java.math.BigDecimal
import java.time.Instant

// KIS WebSocket H0STCNT0(실시간 체결가) 파싱 결과 Value Object
// price: 현재가(시장가 체결 기준), askp1: 매도호가1(지정가 매수 조건), bidp1: 매수호가1(지정가 매도 조건)
data class KisQuoteEvent(
    val ticker: String,
    val price: BigDecimal,
    val askp1: BigDecimal,
    val bidp1: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val volume: BigDecimal,
    val receivedAt: Instant,
)
