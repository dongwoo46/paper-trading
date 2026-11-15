package com.papertrading.api.infrastructure.toss.dto

import java.math.BigDecimal

/**
 * GET /api/v1/holdings 의 result, 공식 명세 1.2.28 HoldingsOverview (필드표 부록 A)
 * 통화별 합계(Price)는 환율 환산 없이 통화마다 따로이고, 쓰는 값만 BigDecimal 이며 안 쓰는 필수 필드는 있는지만 확인함(String)
 */
data class TossHoldingsOverview(
    val totalPurchaseAmount: TossCurrencyAmounts,
    val marketValue: TossOverviewMarketValue,
    val profitLoss: TossOverviewProfitLoss,
    val dailyProfitLoss: TossOverviewDailyProfitLoss,
    val items: List<TossHoldingsItem>,
)

// 명세 Price: krw 필수, usd 는 해외 종목이 없으면 null
data class TossCurrencyAmounts(val krw: BigDecimal, val usd: BigDecimal? = null) {
    // 지원 통화(KRW, USD) 밖은 null, 호출 쪽이 합계 행을 두 통화로 고정
    fun of(currency: String): BigDecimal? = when (currency) {
        KRW -> krw
        USD -> usd
        else -> null
    }

    companion object {
        const val KRW = "KRW"
        const val USD = "USD"
    }
}

data class TossOverviewMarketValue(val amount: TossCurrencyAmounts, val amountAfterCost: TossCurrencyAmounts)

// 합계 rate 는 원화 환산 전체 기준이라 쓰지 않음 (FLAG-07)
data class TossOverviewProfitLoss(
    val amount: TossCurrencyAmounts,
    val amountAfterCost: TossCurrencyAmounts,
    val rate: String,
    val rateAfterCost: String,
)

data class TossOverviewDailyProfitLoss(val amount: TossCurrencyAmounts, val rate: String)
