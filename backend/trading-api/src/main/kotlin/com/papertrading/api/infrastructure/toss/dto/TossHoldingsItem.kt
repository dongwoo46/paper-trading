package com.papertrading.api.infrastructure.toss.dto

import java.math.BigDecimal

/**
 * holdings 의 보유 종목 한 건, 공식 명세 1.2.28 HoldingsItem (필드표 부록 A)
 * currency·marketCountry 는 모르는 값을 허용(문자열), 쓰는 값만 BigDecimal 이고 안 쓰는 필수 필드는 있는지만 확인함(String)
 */
data class TossHoldingsItem(
    val symbol: String,
    val name: String,
    val marketCountry: String,
    val currency: String,
    val quantity: BigDecimal,
    val lastPrice: BigDecimal,
    val averagePurchasePrice: BigDecimal,
    val marketValue: TossItemMarketValue,
    val profitLoss: TossItemProfitLoss,
    val dailyProfitLoss: TossItemDailyProfitLoss,
    val cost: TossItemCost,
)

data class TossItemMarketValue(val purchaseAmount: String, val amount: BigDecimal, val amountAfterCost: String)

// rate 는 소수 비율(0.1077 = 10.77%), 퍼센트로 바꾸는 쪽은 어댑터
data class TossItemProfitLoss(
    val amount: BigDecimal,
    val amountAfterCost: String,
    val rate: BigDecimal,
    val rateAfterCost: String,
)

data class TossItemDailyProfitLoss(val amount: String, val rate: String)

// tax 는 세금이 없으면 null
data class TossItemCost(val commission: String, val tax: String? = null)
