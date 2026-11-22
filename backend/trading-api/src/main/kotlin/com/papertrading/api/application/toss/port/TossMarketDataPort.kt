package com.papertrading.api.application.toss.port

import java.math.BigDecimal

/**
 * 토스 시세 조회 포트 (현재가, 호가, 캔들), 입력 검증은 호출하는 쪽(TossMarketQueryService)이 끝낸 값을 받음
 * 가격과 수량은 BigDecimal, 시각은 토스 원문 문자열 그대로(사람 결정 2026-11-15, 미국 일봉의 -04:00 오프셋을 UTC·KST 로 바꾸지 않음)
 */
interface TossMarketDataPort {
    fun fetchPrices(symbols: List<String>): List<TossPrice>

    fun fetchOrderbook(symbol: String): TossOrderbook

    fun fetchCandles(query: TossCandleQuery): TossCandlePage
}

/** 현재가 한 종목, timestamp 는 체결이 없으면 null, currency 는 모르는 값도 문자열 그대로 */
data class TossPrice(
    val symbol: String,
    val timestamp: String?,
    val lastPrice: BigDecimal,
    val currency: String,
)

/** 호가 한 줄, 가격과 잔량 */
data class TossOrderbookEntry(val price: BigDecimal, val volume: BigDecimal)

/** 호가, asks 는 낮은 가격순, bids 는 높은 가격순으로 토스가 준 순서 그대로, 비어 있을 수 있음 */
data class TossOrderbook(
    val timestamp: String?,
    val currency: String,
    val asks: List<TossOrderbookEntry>,
    val bids: List<TossOrderbookEntry>,
)

/** 봉 단위, code 는 토스 쿼리 값 */
enum class TossCandleInterval(val code: String) {
    ONE_MINUTE("1m"),
    ONE_DAY("1d"),
}

/** 캔들 조회 조건, count·before·adjusted 는 생략하면 토스 기본값을 쓰도록 보내지 않음, before 는 오프셋 포함 ISO 문자열 */
data class TossCandleQuery(
    val symbol: String,
    val interval: TossCandleInterval,
    val count: Int? = null,
    val before: String? = null,
    val adjusted: Boolean? = null,
)

/** 봉 하나, 1m 의 timestamp 는 봉 종료 시각이고 1d 는 현지 자정 */
data class TossCandle(
    val timestamp: String,
    val openPrice: BigDecimal,
    val highPrice: BigDecimal,
    val lowPrice: BigDecimal,
    val closePrice: BigDecimal,
    val volume: BigDecimal,
    val currency: String,
)

/** 캔들 한 쪽, 최신순 그대로, nextBefore 가 null 이면 마지막 쪽 */
data class TossCandlePage(val candles: List<TossCandle>, val nextBefore: String?)
