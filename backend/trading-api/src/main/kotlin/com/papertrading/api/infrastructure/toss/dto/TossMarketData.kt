package com.papertrading.api.infrastructure.toss.dto

import java.math.BigDecimal

/**
 * 시세 API 의 result 원소·본체, 공식 명세 1.2.28 PriceResponse·OrderbookResponse·OrderbookEntry·
 * CandlePageResponse·Candle (필드표는 step-7 부록 A)
 * 가격과 수량은 BigDecimal(문자열 숫자도 정확히), currency 는 모르는 값을 허용(문자열)
 * 시각은 date-time 이지만 문자열로 받음 — Jackson 이 OffsetDateTime 을 UTC 로 바꾸면 미국 일봉 거래일이 흐려져,
 * 형식 검증은 어댑터가 OffsetDateTime.parse 로 함
 * 명세 required 가 아닌 칸(timestamp, nextBefore)만 null 을 허용
 */
data class TossPriceItem(
    val symbol: String,
    val timestamp: String? = null,
    val lastPrice: BigDecimal,
    val currency: String,
)

data class TossOrderbookResult(
    val timestamp: String? = null,
    val currency: String,
    val asks: List<TossOrderbookLine>,
    val bids: List<TossOrderbookLine>,
)

data class TossOrderbookLine(val price: BigDecimal, val volume: BigDecimal)

data class TossCandlePageResult(
    val candles: List<TossCandleItem>,
    val nextBefore: String? = null,
)

data class TossCandleItem(
    val timestamp: String,
    val openPrice: BigDecimal,
    val highPrice: BigDecimal,
    val lowPrice: BigDecimal,
    val closePrice: BigDecimal,
    val volume: BigDecimal,
    val currency: String,
)
