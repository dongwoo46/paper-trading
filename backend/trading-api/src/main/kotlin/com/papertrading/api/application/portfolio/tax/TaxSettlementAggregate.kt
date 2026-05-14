package com.papertrading.api.application.portfolio.tax

import java.math.BigDecimal

/**
 * 연간 TaxSummary 계산을 위한 Settlement 집계 값.
 * - totalRealizedPnl: 실현손익 합계
 * - totalFee: 수수료 합계
 * - totalTax: 거래세 합계 (체결 시점 FeePolicy 반영값)
 * - currency: 정산 통화
 */
data class TaxSettlementAggregate(
    val totalRealizedPnl: BigDecimal,
    val totalFee: BigDecimal,
    val totalTax: BigDecimal,
    val currency: String,
)

