package com.papertrading.collector.domain.entity.kis

import java.math.BigDecimal
import java.time.Instant

// KIS WebSocket H0STASP0(실시간 호가잔량) 파싱 결과 Value Object
// askPrices[0] = 최우선 매도호가(best ask), bidPrices[0] = 최우선 매수호가(best bid)
data class KisOrderbookEvent(
    val ticker: String,
    val askPrices: List<BigDecimal>,
    val bidPrices: List<BigDecimal>,
    val askQtys: List<BigDecimal>,
    val bidQtys: List<BigDecimal>,
    val receivedAt: Instant,
)
