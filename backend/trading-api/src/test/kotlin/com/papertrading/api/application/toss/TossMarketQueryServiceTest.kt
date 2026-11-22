package com.papertrading.api.application.toss

import com.papertrading.api.application.toss.port.TossCandleInterval
import com.papertrading.api.application.toss.port.TossCandlePage
import com.papertrading.api.application.toss.port.TossCandleQuery
import com.papertrading.api.application.toss.port.TossMarketDataPort
import com.papertrading.api.application.toss.port.TossOrderbook
import com.papertrading.api.common.exception.InvalidMarketQueryException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource

// 시세 입력 검증: 우리 쪽에서 먼저 400 INVALID_MARKET_QUERY, 토스를 부르지 않고 문구에 입력값을 싣지 않음 (D9, 부록 A)
class TossMarketQueryServiceTest {

    private val port = mockk<TossMarketDataPort>()
    private val service = TossMarketQueryService(port)
    private val emptyBook = TossOrderbook(null, "KRW", emptyList(), emptyList())
    private val emptyPage = TossCandlePage(emptyList(), null)

    private fun rejected(block: () -> Unit): InvalidMarketQueryException {
        val error = assertThrows<InvalidMarketQueryException>(block)
        assertEquals("INVALID_MARKET_QUERY", error.errorCode)
        verify(exactly = 0) { port.fetchPrices(any()) }
        verify(exactly = 0) { port.fetchOrderbook(any()) }
        verify(exactly = 0) { port.fetchCandles(any()) }
        return error
    }

    private fun symbols(count: Int) = (1..count).joinToString(",") { "S$it" }

    // ---- prices: symbols ----

    @ParameterizedTest(name = "symbols [{0}] 은 거부")
    @NullAndEmptySource
    @ValueSource(
        strings = [",", "A,,B", "A,", ",A", " ", "A, B", "A B", "A/B", "A_B", "A;B", "A%", "A+B", "가나", "A\n", "A\tB"],
    )
    fun `symbols 가 없거나 빈 원소가 있거나 패턴 밖 문자가 있으면 400 이다`(symbols: String?) {
        rejected { service.getPrices(symbols) }
    }

    @Test
    fun `symbols 는 1개와 200개를 허용한다`() {
        every { port.fetchPrices(any()) } returns emptyList()

        service.getPrices("005930")
        service.getPrices(symbols(200))

        verify(exactly = 1) { port.fetchPrices(listOf("005930")) }
        verify(exactly = 1) { port.fetchPrices((1..200).map { "S$it" }) }
    }

    @Test
    fun `symbols 201개는 거부한다`() {
        rejected { service.getPrices(symbols(201)) }
    }

    @Test
    fun `symbols 는 영문 대소문자 숫자 점 하이픈을 허용하고 입력 순서대로 넘긴다`() {
        every { port.fetchPrices(any()) } returns emptyList()

        service.getPrices("BRK.B,BF-B,aapl,005930")

        verify(exactly = 1) { port.fetchPrices(listOf("BRK.B", "BF-B", "aapl", "005930")) }
    }

    // ---- orderbook: symbol ----

    @ParameterizedTest(name = "symbol [{0}] 은 거부")
    @NullAndEmptySource
    @ValueSource(strings = [" ", "A,B", "A B", "A/B", "A_B", "가나", "A\n"])
    fun `orderbook symbol 이 없거나 패턴 밖이면 400 이다`(symbol: String?) {
        rejected { service.getOrderbook(symbol) }
    }

    @Test
    fun `orderbook symbol 이 맞으면 포트로 넘긴다`() {
        every { port.fetchOrderbook("BRK.B") } returns emptyBook

        assertEquals(emptyBook, service.getOrderbook("BRK.B"))
    }

    // ---- candles ----

    private val query = mutableListOf<TossCandleQuery>()

    private fun candles(
        symbol: String? = "005930",
        interval: String? = "1d",
        count: String? = null,
        before: String? = null,
        adjusted: String? = null,
    ): TossCandlePage = service.getCandles(symbol, interval, count, before, adjusted)

    private fun stubCandles() {
        every { port.fetchCandles(capture(query)) } returns emptyPage
    }

    @ParameterizedTest(name = "candles symbol [{0}] 은 거부")
    @NullAndEmptySource
    @ValueSource(strings = ["A,B", "A B", "A/B"])
    fun `candles symbol 이 없거나 패턴 밖이면 400 이다`(symbol: String?) {
        rejected { candles(symbol = symbol) }
    }

    @ParameterizedTest(name = "interval [{0}] 은 거부")
    @NullAndEmptySource
    @ValueSource(strings = ["5m", "1h", "1D", "1M", " 1d", "1d ", "minute", "day", "1"])
    fun `interval 이 1m 1d 밖이면 400 이다`(interval: String?) {
        rejected { candles(interval = interval) }
    }

    @Test
    fun `interval 1m 과 1d 는 봉 단위로 바꿔 넘긴다`() {
        stubCandles()

        candles(interval = "1m")
        candles(interval = "1d")

        assertEquals(listOf(TossCandleInterval.ONE_MINUTE, TossCandleInterval.ONE_DAY), query.map { it.interval })
    }

    @ParameterizedTest(name = "count [{0}] 은 거부")
    @ValueSource(strings = ["0", "201", "-1", "abc", "", " ", "1.5", "+5", " 5", "5 ", "99999999999", "١٠"])
    fun `count 가 1 에서 200 밖이거나 정수가 아니면 400 이다`(count: String) {
        rejected { candles(count = count) }
    }

    @ParameterizedTest(name = "count [{0}] 은 허용")
    @ValueSource(strings = ["1", "2", "100", "199", "200", "007"])
    fun `count 1 에서 200 은 허용한다`(count: String) {
        stubCandles()

        candles(count = count)

        assertEquals(count.toInt(), query.single().count)
    }

    @Test
    fun `count 를 생략하면 토스 기본값에 맡겨 null 로 넘긴다`() {
        stubCandles()

        candles()

        assertEquals(TossCandleQuery("005930", TossCandleInterval.ONE_DAY, null, null, null), query.single())
    }

    @ParameterizedTest(name = "before [{0}] 은 거부")
    @ValueSource(
        strings = [
            "", " ", "2026-03-25", "2026-03-25T09:00:00", "2026-03-25T09:00:00 09:00", "2026-03-25 09:00:00+09:00",
            "yesterday", "2026-13-25T09:00:00+09:00", "1711324800",
        ],
    )
    fun `오프셋 없는 before 나 ISO 시각이 아닌 before 는 400 이다`(before: String) {
        rejected { candles(before = before) }
    }

    @ParameterizedTest(name = "before [{0}] 은 원문 그대로 넘긴다")
    @ValueSource(
        strings = [
            "2026-03-25T09:00:00+09:00", "2026-03-25T00:00:00-04:00",
            "2026-03-25T09:00:00.123+09:00", "2026-03-25T00:00:00Z",
        ],
    )
    fun `오프셋이 있는 ISO before 는 변환하지 않고 원문 그대로 넘긴다`(before: String) {
        stubCandles()

        candles(before = before)

        assertEquals(before, query.single().before)
    }

    @ParameterizedTest(name = "adjusted [{0}] 은 거부")
    @ValueSource(strings = ["", " ", "1", "0", "yes", "TRUE", "True", "null"])
    fun `adjusted 가 true false 가 아니면 400 이다`(adjusted: String) {
        rejected { candles(adjusted = adjusted) }
    }

    @Test
    fun `adjusted true false 는 불린으로 넘긴다`() {
        stubCandles()

        candles(adjusted = "true")
        candles(adjusted = "false")

        assertEquals(listOf(true, false), query.map { it.adjusted })
    }

    @Test
    fun `캔들 조건을 모두 주면 그대로 한 조건으로 묶어 넘긴다`() {
        stubCandles()

        candles(
            symbol = "AAPL",
            interval = "1m",
            count = "50",
            before = "2026-03-25T09:00:00+09:00",
            adjusted = "false",
        )

        val expected = TossCandleQuery("AAPL", TossCandleInterval.ONE_MINUTE, 50, "2026-03-25T09:00:00+09:00", false)
        assertEquals(expected, query.single())
    }

    // ---- 문구에 입력값 없음 ----

    @Test
    fun `거부 문구에는 입력값이 없고 어느 입력이 어떤 규칙을 어겼는지만 있다`() {
        val sentinel = "SECRET-INPUT-VALUE#"
        val errors = mapOf(
            "symbols" to rejected { service.getPrices("A,$sentinel") },
            "symbol" to rejected { service.getOrderbook(sentinel) },
            "interval" to rejected { candles(interval = sentinel) },
            "count" to rejected { candles(count = sentinel) },
            "before" to rejected { candles(before = sentinel) },
            "adjusted" to rejected { candles(adjusted = sentinel) },
        )

        errors.forEach { (field, error) ->
            assertFalse(error.message.orEmpty().contains(sentinel), error.message)
            assertFalse(error.message.orEmpty().contains("SECRET"), error.message)
            assertTrue(error.message.orEmpty().contains("입력=$field,"), error.message)
        }
    }
}
