package com.papertrading.api.infrastructure.toss.dto

import java.math.BigDecimal

/** GET /api/v1/buying-power 의 result, 공식 명세 1.2.28 BuyingPowerResponse, currency 는 모르는 값을 허용(문자열) */
data class TossBuyingPower(val currency: String, val cashBuyingPower: BigDecimal)
