package com.papertrading.api.infrastructure.toss

import com.fasterxml.jackson.core.type.TypeReference
import com.papertrading.api.application.toss.port.TossCandle
import com.papertrading.api.application.toss.port.TossCandlePage
import com.papertrading.api.application.toss.port.TossCandleQuery
import com.papertrading.api.application.toss.port.TossMarketDataPort
import com.papertrading.api.application.toss.port.TossOrderbook
import com.papertrading.api.application.toss.port.TossOrderbookEntry
import com.papertrading.api.application.toss.port.TossPrice
import com.papertrading.api.common.exception.TossResponseInvalidException
import com.papertrading.api.infrastructure.toss.dto.TossCandleItem
import com.papertrading.api.infrastructure.toss.dto.TossCandlePageResult
import com.papertrading.api.infrastructure.toss.dto.TossOrderbookLine
import com.papertrading.api.infrastructure.toss.dto.TossOrderbookResult
import com.papertrading.api.infrastructure.toss.dto.TossPriceItem
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * 토스 시세 어댑터 (D9): GET /api/v1/prices·orderbook (MARKET_DATA), /api/v1/candles (MARKET_DATA_CHART)
 * 계좌 헤더 없음, 필드표는 step-7 부록 A
 * 가격과 수량은 BigDecimal 로 읽고, 시각은 OffsetDateTime.parse 로 형식만 검증한 뒤 토스 원문 문자열 그대로 돌려줌(사람 결정 2026-11-15)
 * 필수 필드 누락·숫자 변환 실패·시각 형식 오류는 대체값 없이 TOSS_RESPONSE_INVALID (D14), 빈 목록은 정상, 목록 순서는 토스 그대로
 * 입력 검증은 TossMarketQueryService 몫이라 여기서는 받은 값을 그대로 요청에 싣고, 요청 입력값은 응답 해석 실패 사유에 싣지 않음
 */
@Component
class TossMarketDataClient(
    private val client: TossApiClient,
) : TossMarketDataPort {

    override fun fetchPrices(symbols: List<String>): List<TossPrice> {
        val query = mapOf("symbols" to symbols.joinToString(","))
        val request = TossApiRequest(PRICES_PATH, TossRateLimitGroup.MARKET_DATA, query)
        return client.get(request, PRICES_TYPE).map(::toPrice)
    }

    override fun fetchOrderbook(symbol: String): TossOrderbook {
        val request = TossApiRequest(ORDERBOOK_PATH, TossRateLimitGroup.MARKET_DATA, mapOf("symbol" to symbol))
        return toOrderbook(client.get(request, TossOrderbookResult::class.java))
    }

    override fun fetchCandles(query: TossCandleQuery): TossCandlePage {
        val request = TossApiRequest(CANDLES_PATH, TossRateLimitGroup.MARKET_DATA_CHART, candleParams(query))
        return toCandlePage(client.get(request, TossCandlePageResult::class.java))
    }

    // 생략한 값은 보내지 않아 토스 기본값(count 100, adjusted true, before 없음 = 최신부터)을 씀
    private fun candleParams(query: TossCandleQuery): Map<String, String> = buildMap {
        put("symbol", query.symbol)
        put("interval", query.interval.code)
        query.count?.let { put("count", it.toString()) }
        query.before?.let { put("before", it) }
        query.adjusted?.let { put("adjusted", it.toString()) }
    }

    private fun toPrice(item: TossPriceItem) = TossPrice(
        symbol = item.symbol,
        timestamp = item.timestamp?.let { checkedTimestamp(it) },
        lastPrice = item.lastPrice,
        currency = item.currency,
    )

    private fun toOrderbook(result: TossOrderbookResult) = TossOrderbook(
        timestamp = result.timestamp?.let { checkedTimestamp(it) },
        currency = result.currency,
        asks = result.asks.map(::toEntry),
        bids = result.bids.map(::toEntry),
    )

    private fun toEntry(line: TossOrderbookLine) = TossOrderbookEntry(price = line.price, volume = line.volume)

    private fun toCandlePage(result: TossCandlePageResult) = TossCandlePage(
        candles = result.candles.map(::toCandle),
        nextBefore = result.nextBefore?.let { checkedTimestamp(it) },
    )

    private fun toCandle(item: TossCandleItem) = TossCandle(
        timestamp = checkedTimestamp(item.timestamp),
        openPrice = item.openPrice,
        highPrice = item.highPrice,
        lowPrice = item.lowPrice,
        closePrice = item.closePrice,
        volume = item.volume,
        currency = item.currency,
    )

    // 형식만 확인하고 원문을 그대로 돌려줌, 파싱한 값으로 바꾸면 오프셋이 달라져 미국 일봉 거래일이 흐려짐, 값은 사유에 싣지 않음
    private fun checkedTimestamp(raw: String): String {
        try {
            OffsetDateTime.parse(raw)
        } catch (_: DateTimeParseException) {
            // 파싱 예외 문구에 응답 원문이 들어 있어 원인으로 잇지 않음
            throw TossResponseInvalidException("시각 형식 오류")
        }
        return raw
    }

    companion object {
        private const val PRICES_PATH = "/api/v1/prices"
        private const val ORDERBOOK_PATH = "/api/v1/orderbook"
        private const val CANDLES_PATH = "/api/v1/candles"
        private val PRICES_TYPE = object : TypeReference<List<TossPriceItem>>() {}
    }
}
