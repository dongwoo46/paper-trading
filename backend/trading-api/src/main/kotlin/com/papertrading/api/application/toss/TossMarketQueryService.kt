package com.papertrading.api.application.toss

import com.papertrading.api.application.toss.port.TossCandleInterval
import com.papertrading.api.application.toss.port.TossCandlePage
import com.papertrading.api.application.toss.port.TossCandleQuery
import com.papertrading.api.application.toss.port.TossMarketDataPort
import com.papertrading.api.application.toss.port.TossOrderbook
import com.papertrading.api.application.toss.port.TossPrice
import com.papertrading.api.common.exception.InvalidMarketQueryException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * 토스 시세 조회 유스케이스, 외부 호출만 하므로 트랜잭션 없음
 * 입력은 문자열 그대로 받아 공식 명세(1.2.28) 규칙으로 우리 쪽에서 먼저 검증하고 400 INVALID_MARKET_QUERY, 토스를 부르기 전에 막음
 * 문구에는 어느 입력이 어떤 규칙을 어겼는지만 싣고 입력값은 싣지 않음, 값 변환은 하지 않고 before 는 검증만 한 원문을 넘김
 */
@Service
class TossMarketQueryService(
    private val tossMarketDataPort: TossMarketDataPort,
) {
    fun getPrices(symbols: String?): List<TossPrice> = tossMarketDataPort.fetchPrices(parseSymbols(symbols))

    fun getOrderbook(symbol: String?): TossOrderbook {
        val validSymbol = requireSymbol("symbol", symbol)
        return tossMarketDataPort.fetchOrderbook(validSymbol)
    }

    @Suppress("LongParameterList")
    fun getCandles(
        symbol: String?,
        interval: String?,
        count: String?,
        before: String?,
        adjusted: String?,
    ): TossCandlePage = tossMarketDataPort.fetchCandles(
        TossCandleQuery(
            symbol = requireSymbol("symbol", symbol),
            interval = parseInterval(interval),
            count = count?.let(::parseCount),
            before = before?.let(::requireOffsetDateTime),
            adjusted = adjusted?.let(::parseAdjusted),
        ),
    )

    // 빈 원소("A,,B", 끝 쉼표)는 심볼 패턴이 거부하므로 split 이 남기는 빈 문자열로 걸러짐
    private fun parseSymbols(symbols: String?): List<String> {
        val items = symbols?.split(SYMBOL_SEPARATOR) ?: throw InvalidMarketQueryException("symbols", "필수")
        if (items.size > MAX_SYMBOLS || !items.all(SYMBOL_PATTERN::matches)) {
            throw InvalidMarketQueryException("symbols", "최대 ${MAX_SYMBOLS}개, 원소는 영문 대소문자 숫자 점 하이픈만, 빈 원소 불가")
        }
        return items
    }

    private fun requireSymbol(field: String, symbol: String?): String {
        if (symbol == null) throw InvalidMarketQueryException(field, "필수")
        if (!SYMBOL_PATTERN.matches(symbol)) throw InvalidMarketQueryException(field, "영문 대소문자 숫자 점 하이픈만")
        return symbol
    }

    private fun parseInterval(interval: String?): TossCandleInterval {
        if (interval == null) throw InvalidMarketQueryException("interval", "필수")
        return TossCandleInterval.entries.firstOrNull { it.code == interval }
            ?: throw InvalidMarketQueryException("interval", "1m 또는 1d")
    }

    private fun parseCount(count: String): Int {
        val value = if (COUNT_PATTERN.matches(count)) count.toIntOrNull() else null
        if (value == null || value !in MIN_COUNT..MAX_COUNT) {
            throw InvalidMarketQueryException("count", "$MIN_COUNT 이상 $MAX_COUNT 이하 정수")
        }
        return value
    }

    // 형식만 검증하고 원문을 그대로 넘김, 오프셋이 없거나 ISO 가 아니면 거부
    private fun requireOffsetDateTime(before: String): String {
        try {
            OffsetDateTime.parse(before)
        } catch (_: DateTimeParseException) {
            throw InvalidMarketQueryException("before", "오프셋을 포함한 ISO 시각, + 는 %2B 로 인코딩")
        }
        return before
    }

    private fun parseAdjusted(adjusted: String): Boolean = when (adjusted) {
        "true" -> true
        "false" -> false
        else -> throw InvalidMarketQueryException("adjusted", "true 또는 false")
    }

    private companion object {
        const val SYMBOL_SEPARATOR = ","
        const val MAX_SYMBOLS = 200
        const val MIN_COUNT = 1
        const val MAX_COUNT = 200

        // 공식 명세의 종목 패턴, 전체 일치로만 쓰므로 앵커 없이 줄바꿈이 끼어도 거부됨
        val SYMBOL_PATTERN = Regex("[A-Za-z0-9.\\-]+")
        val COUNT_PATTERN = Regex("[0-9]+")
    }
}
