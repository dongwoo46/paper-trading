package com.papertrading.api.infrastructure.toss

import com.papertrading.api.application.toss.port.TossCandleInterval
import com.papertrading.api.application.toss.port.TossCandleQuery
import com.papertrading.api.common.exception.TossIntegrationDisabledException
import com.papertrading.api.common.exception.TossRequestRejectedException
import com.papertrading.api.common.exception.TossResponseInvalidException
import com.papertrading.api.common.exception.TossUnavailableException
import com.papertrading.api.infrastructure.toss.TossFixtures.reply
import com.papertrading.api.infrastructure.toss.TossFixtures.replyRaw
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.ResponseActions
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.util.Base64
import java.util.concurrent.atomic.AtomicLong

// 토스 시세 어댑터: GET /api/v1/prices·orderbook (MARKET_DATA), /api/v1/candles (MARKET_DATA_CHART), 계좌 헤더 없음
// 필드는 step-7 부록 A, 가격과 수량은 BigDecimal, 시각은 토스 원문 문자열, 누락·변환 실패는 TOSS_RESPONSE_INVALID (D9, D13, D14)
class TossMarketDataClientTest {

    private val restTemplate = RestTemplate()
    private val server = MockRestServiceServer.bindTo(restTemplate).build()
    private val tokenManager = mockk<TossTokenManager>()

    // 시험이 정한 임의 값, 토스 정책 수치가 아님, 시세 두 그룹만 간격이 보이게 낮춤(MARKET_DATA 200ms, MARKET_DATA_CHART 100ms)
    private val properties = TossProperties().apply {
        baseUrl = "https://toss.example.invalid"
        clientId = "fake-client-id"
        clientSecret = "fake-client-secret"
        tokenEncryptionKey = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })
        rateLimits = TossRateLimitGroup.entries.associateWith { 1_000 } +
            mapOf(TossRateLimitGroup.MARKET_DATA to 5, TossRateLimitGroup.MARKET_DATA_CHART to 10)
    }

    // 실제 리미터와 주입 시계, 잠은 시계를 움직일 뿐 실제로 자지 않고 기록해 그룹 간격을 관찰
    private val clock = AtomicLong(1_000)
    private val slept = mutableListOf<Long>()
    private val limiter = TossRateLimiter(properties, clock::get) { millis ->
        slept += millis
        clock.addAndGet(millis)
    }
    private val marketClient = TossMarketDataClient(TossApiClient(properties, tokenManager, limiter, restTemplate))

    init {
        every { tokenManager.getToken() } returns "fake-token"
    }

    private val dailyPath = "/candles?symbol=005930&interval=1d"

    // 경로와 쿼리만 적으면 기본 주소 아래 /api/v1 로 이어 붙임
    private fun expectGet(pathAndQuery: String): ResponseActions {
        val expected = server.expect(requestTo("https://toss.example.invalid/api/v1$pathAndQuery"))
        return expected
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer fake-token"))
            .andExpect(headerDoesNotExist("X-Tossinvest-Account"))
    }

    private fun candleQuery(
        interval: TossCandleInterval = TossCandleInterval.ONE_DAY,
        count: Int? = null,
        before: String? = null,
        adjusted: Boolean? = null,
    ) = TossCandleQuery("005930", interval, count, before, adjusted)

    private fun assertDecimal(expected: String, actual: BigDecimal) {
        assertEquals(0, BigDecimal(expected).compareTo(actual), "기대 $expected 실제 $actual")
        assertEquals(expected, actual.toPlainString())
    }

    // ---- prices ----

    @Test
    fun `국내 현재가를 읽어 가격은 BigDecimal 시각은 토스 원문 문자열로 돌려준다`() {
        expectGet("/prices?symbols=005930").andRespond(reply(HttpStatus.OK, "success-prices-kr.json"))

        val price = marketClient.fetchPrices(listOf("005930")).single()

        assertEquals("005930", price.symbol)
        assertEquals("2026-03-25T09:30:00.123+09:00", price.timestamp)
        assertDecimal("72000", price.lastPrice)
        assertEquals("KRW", price.currency)
        server.verify()
    }

    @Test
    fun `해외 현재가의 소수 자릿수를 그대로 보존한다`() {
        expectGet("/prices?symbols=AAPL").andRespond(reply(HttpStatus.OK, "success-prices-us.json"))

        val price = marketClient.fetchPrices(listOf("AAPL")).single()

        assertDecimal("185.70", price.lastPrice)
        assertEquals("USD", price.currency)
        assertEquals("2026-03-25T22:30:00.456+09:00", price.timestamp)
    }

    @Test
    fun `여러 종목은 쉼표로 이어 한 번에 요청하고 토스가 준 순서 그대로 돌려준다`() {
        val aapl = priceItem("symbol" to "\"AAPL\"", "lastPrice" to "\"185.70\"", "currency" to "\"USD\"")
        val samsung = priceItem()
        expectGet("/prices?symbols=005930,AAPL").andRespond(replyRaw(HttpStatus.OK, """{"result":[$aapl,$samsung]}"""))

        val prices = marketClient.fetchPrices(listOf("005930", "AAPL"))

        assertEquals(listOf("AAPL", "005930"), prices.map { it.symbol })
        server.verify()
    }

    @Test
    fun `result 가 빈 배열이면 정상으로 빈 목록이다`() {
        expectGet("/prices?symbols=005930").andRespond(reply(HttpStatus.OK, "success-prices-empty.json"))

        assertTrue(marketClient.fetchPrices(listOf("005930")).isEmpty())
    }

    @Test
    fun `timestamp 가 null 이면 null 그대로 돌려준다`() {
        expectGet("/prices?symbols=005930").andRespond(reply(HttpStatus.OK, "success-prices-timestamp-null.json"))

        assertNull(marketClient.fetchPrices(listOf("005930")).single().timestamp)
    }

    @Test
    fun `모르는 currency 문자열은 그대로 돌려준다`() {
        val body = """{"result":[${priceItem("currency" to "\"JPY\"")}]}"""
        expectGet("/prices?symbols=005930").andRespond(replyRaw(HttpStatus.OK, body))

        assertEquals("JPY", marketClient.fetchPrices(listOf("005930")).single().currency)
    }

    @Test
    fun `정수부 15자리 소수 6자리 가격도 자릿수를 잃지 않는다`() {
        val big = priceItem("symbol" to "\"X\"", "lastPrice" to "\"123456789012345.123456\"")
        val small = priceItem("symbol" to "\"Y\"", "lastPrice" to "\"0.000001\"")
        expectGet("/prices?symbols=X,Y").andRespond(replyRaw(HttpStatus.OK, """{"result":[$big,$small]}"""))

        val prices = marketClient.fetchPrices(listOf("X", "Y"))

        assertDecimal("123456789012345.123456", prices[0].lastPrice)
        assertDecimal("0.000001", prices[1].lastPrice)
    }

    @ParameterizedTest(name = "현재가 본문 {0} 은 TOSS_RESPONSE_INVALID")
    @MethodSource("invalidPriceBodies")
    fun `현재가의 필수 필드 누락 변환 실패 시각 형식 오류는 TOSS_RESPONSE_INVALID 이다`(label: String, body: String) {
        expectGet("/prices?symbols=005930").andRespond(replyRaw(HttpStatus.OK, body))

        assertThrows<TossResponseInvalidException>(label) { marketClient.fetchPrices(listOf("005930")) }
    }

    // ---- orderbook ----

    @Test
    fun `국내 호가를 토스가 준 순서 그대로 읽는다`() {
        expectGet("/orderbook?symbol=005930").andRespond(reply(HttpStatus.OK, "success-orderbook-kr.json"))

        val book = marketClient.fetchOrderbook("005930")

        assertEquals("2026-03-25T09:30:00.123+09:00", book.timestamp)
        assertEquals("KRW", book.currency)
        assertEquals(listOf("72300", "72200", "72100"), book.asks.map { it.price.toPlainString() })
        assertEquals(listOf("1200", "3400", "8500"), book.asks.map { it.volume.toPlainString() })
        assertEquals(listOf("72000", "71900", "71800"), book.bids.map { it.price.toPlainString() })
        assertEquals(listOf("5200", "4100", "2700"), book.bids.map { it.volume.toPlainString() })
        server.verify()
    }

    @Test
    fun `해외 호가의 소수 자릿수를 보존한다`() {
        expectGet("/orderbook?symbol=AAPL").andRespond(reply(HttpStatus.OK, "success-orderbook-us.json"))

        val book = marketClient.fetchOrderbook("AAPL")

        assertEquals("USD", book.currency)
        assertEquals(listOf("185.75", "185.70"), book.asks.map { it.price.toPlainString() })
        assertEquals(listOf("185.65", "185.60"), book.bids.map { it.price.toPlainString() })
    }

    @Test
    fun `호가가 비어 있고 timestamp 가 null 이어도 정상이다`() {
        expectGet("/orderbook?symbol=005930").andRespond(reply(HttpStatus.OK, "success-orderbook-empty.json"))

        val book = marketClient.fetchOrderbook("005930")

        assertNull(book.timestamp)
        assertTrue(book.asks.isEmpty())
        assertTrue(book.bids.isEmpty())
    }

    @ParameterizedTest(name = "호가 본문 {0} 은 TOSS_RESPONSE_INVALID")
    @MethodSource("invalidOrderbookBodies")
    fun `호가의 필수 필드 누락 변환 실패 시각 형식 오류는 TOSS_RESPONSE_INVALID 이다`(label: String, body: String) {
        expectGet("/orderbook?symbol=005930").andRespond(replyRaw(HttpStatus.OK, body))

        assertThrows<TossResponseInvalidException>(label) { marketClient.fetchOrderbook("005930") }
    }

    // ---- candles ----

    @Test
    fun `일봉은 count 와 before 와 adjusted 를 생략하면 보내지 않고 최신순 그대로 읽는다`() {
        expectGet(dailyPath).andRespond(reply(HttpStatus.OK, "success-candles-daily-kr.json"))

        val page = marketClient.fetchCandles(candleQuery())

        assertEquals(
            listOf("2026-03-25T00:00:00+09:00", "2026-03-24T00:00:00+09:00"),
            page.candles.map { it.timestamp },
        )
        assertEquals("2026-03-24T00:00:00+09:00", page.nextBefore)
        val first = page.candles.first()
        assertDecimal("71600", first.openPrice)
        assertDecimal("72300", first.highPrice)
        assertDecimal("71500", first.lowPrice)
        assertDecimal("72000", first.closePrice)
        assertDecimal("3521000", first.volume)
        assertEquals("KRW", first.currency)
        server.verify()
    }

    @Test
    fun `분봉은 count 와 before 와 adjusted 를 보내고 before 의 + 는 퍼센트 인코딩된 2B 로 나간다`() {
        val query = "symbol=005930&interval=1m&count=2&before=2026-03-25T09:32:00%2B09:00&adjusted=false"
        expectGet("/candles?$query").andRespond(reply(HttpStatus.OK, "success-candles-minute-kr.json"))

        val before = "2026-03-25T09:32:00+09:00"
        val page = marketClient.fetchCandles(candleQuery(TossCandleInterval.ONE_MINUTE, 2, before, adjusted = false))

        assertEquals(
            listOf("2026-03-25T09:32:00+09:00", "2026-03-25T09:31:00+09:00"),
            page.candles.map { it.timestamp },
        )
        assertEquals("2026-03-25T09:31:00+09:00", page.nextBefore)
        server.verify()
    }

    @Test
    fun `adjusted 가 true 면 그대로 보낸다`() {
        expectGet("/candles?symbol=005930&interval=1d&adjusted=true")
            .andRespond(reply(HttpStatus.OK, "success-candles-last-page.json"))

        marketClient.fetchCandles(candleQuery(adjusted = true))

        server.verify()
    }

    @Test
    fun `마지막 쪽은 nextBefore 가 null 이다`() {
        expectGet(dailyPath).andRespond(reply(HttpStatus.OK, "success-candles-last-page.json"))

        val page = marketClient.fetchCandles(candleQuery())

        assertNull(page.nextBefore)
        assertEquals(1, page.candles.size)
    }

    @Test
    fun `빈 캔들 목록은 정상이다`() {
        val body = """{"result":{"candles":[],"nextBefore":null}}"""
        expectGet(dailyPath).andRespond(replyRaw(HttpStatus.OK, body))

        val page = marketClient.fetchCandles(candleQuery())

        assertTrue(page.candles.isEmpty())
        assertNull(page.nextBefore)
    }

    @Test
    fun `미국 일봉의 -04 00 오프셋을 UTC 나 KST 로 바꾸지 않고 원문 그대로 돌려준다`() {
        expectGet(dailyPath).andRespond(reply(HttpStatus.OK, "success-candles-daily-us.json"))

        val page = marketClient.fetchCandles(candleQuery())

        assertEquals(
            listOf("2026-03-25T00:00:00-04:00", "2026-03-24T00:00:00-04:00"),
            page.candles.map { it.timestamp },
        )
        assertEquals("2026-03-24T00:00:00-04:00", page.nextBefore)
        assertDecimal("184.10", page.candles.first().openPrice)
        assertEquals("USD", page.candles.first().currency)
    }

    @Test
    fun `정수부 15자리 소수 6자리 캔들 가격도 자릿수를 잃지 않는다`() {
        val big = "\"123456789012345.123456\""
        val candle = candleItem("openPrice" to big, "highPrice" to big, "lowPrice" to "\"0.000001\"", "volume" to big)
        val body = """{"result":{"candles":[$candle],"nextBefore":null}}"""
        expectGet(dailyPath).andRespond(replyRaw(HttpStatus.OK, body))

        val first = marketClient.fetchCandles(candleQuery()).candles.single()

        assertDecimal("123456789012345.123456", first.openPrice)
        assertDecimal("0.000001", first.lowPrice)
        assertDecimal("123456789012345.123456", first.volume)
    }

    @ParameterizedTest(name = "캔들에서 {0} 를 빼면 TOSS_RESPONSE_INVALID")
    @CsvSource("timestamp", "openPrice", "highPrice", "lowPrice", "closePrice", "volume", "currency")
    fun `캔들의 필수 필드를 하나씩 빼면 TOSS_RESPONSE_INVALID 이다`(missing: String) {
        val body = """{"result":{"candles":[${candleItem(missing to null)}],"nextBefore":null}}"""
        expectGet(dailyPath).andRespond(replyRaw(HttpStatus.OK, body))

        assertThrows<TossResponseInvalidException>(missing) { marketClient.fetchCandles(candleQuery()) }
    }

    @ParameterizedTest(name = "캔들 본문 {0} 은 TOSS_RESPONSE_INVALID")
    @MethodSource("invalidCandleBodies")
    fun `캔들의 누락 변환 실패 시각 형식 오류는 TOSS_RESPONSE_INVALID 이다`(label: String, body: String) {
        expectGet(dailyPath).andRespond(replyRaw(HttpStatus.OK, body))

        assertThrows<TossResponseInvalidException>(label) { marketClient.fetchCandles(candleQuery()) }
    }

    // ---- 한도 그룹, 오류 분류, 비활성 ----

    @Test
    fun `현재가와 호가는 MARKET_DATA 캔들은 MARKET_DATA_CHART 그룹의 간격을 따른다`() {
        expectGet("/prices?symbols=005930").andRespond(reply(HttpStatus.OK, "success-prices-kr.json"))
        expectGet("/orderbook?symbol=005930").andRespond(reply(HttpStatus.OK, "success-orderbook-kr.json"))
        expectGet(dailyPath).andRespond(reply(HttpStatus.OK, "success-candles-last-page.json"))
        expectGet(dailyPath).andRespond(reply(HttpStatus.OK, "success-candles-last-page.json"))

        marketClient.fetchPrices(listOf("005930"))
        marketClient.fetchOrderbook("005930")
        // devkit:test-change S7-04 TT7·TT44 목 리미터의 그룹별 호출 횟수 단언을 실제 리미터가 만드는 그룹 간격 관찰로 바꿈, 그룹 구분 기대는 그대로
        assertEquals(listOf(200L), slept, "현재가 뒤 호가는 같은 그룹이라 MARKET_DATA 간격 200ms 를 기다림")
        marketClient.fetchCandles(candleQuery())
        assertEquals(listOf(200L), slept, "캔들의 첫 호출은 다른 그룹이라 기다리지 않음")
        marketClient.fetchCandles(candleQuery())

        assertEquals(listOf(200L, 100L), slept, "캔들 둘째 호출은 MARKET_DATA_CHART 간격 100ms")
        server.verify()
    }

    @Test
    fun `토스의 404 종목 없음은 TOSS_REQUEST_REJECTED 이고 500 은 TOSS_UNAVAILABLE 이다`() {
        expectGet("/orderbook?symbol=ZZZZ").andRespond(reply(HttpStatus.NOT_FOUND, "error-stock-not-found.json"))
        assertThrows<TossRequestRejectedException> { marketClient.fetchOrderbook("ZZZZ") }

        server.reset()
        expectGet("/orderbook?symbol=ZZZZ")
            .andRespond(reply(HttpStatus.INTERNAL_SERVER_ERROR, "error-internal-error.json"))
        assertThrows<TossUnavailableException> { marketClient.fetchOrderbook("ZZZZ") }
    }

    @Test
    fun `토스 설정이 비활성이면 호출 없이 TOSS_INTEGRATION_DISABLED 이다`() {
        val disabled = TossMarketDataClient(TossApiClient(TossProperties(), tokenManager, limiter, restTemplate))

        assertThrows<TossIntegrationDisabledException> { disabled.fetchPrices(listOf("005930")) }
        assertThrows<TossIntegrationDisabledException> { disabled.fetchOrderbook("005930") }
        assertThrows<TossIntegrationDisabledException> { disabled.fetchCandles(candleQuery()) }

        server.verify()
    }

    companion object {
        // 값은 JSON 조각 그대로(문자열이면 따옴표 포함), null 을 주면 그 필드를 뺌
        private fun jsonObject(base: Map<String, String>, overrides: Array<out Pair<String, String?>>): String {
            val merged = LinkedHashMap(base)
            overrides.forEach { (field, value) -> if (value == null) merged.remove(field) else merged[field] = value }
            return merged.entries.joinToString(",", "{", "}") { (field, value) -> "\"$field\":$value" }
        }

        private fun priceItem(vararg overrides: Pair<String, String?>) = jsonObject(
            linkedMapOf(
                "symbol" to "\"005930\"",
                "timestamp" to "\"2026-03-25T09:30:00.123+09:00\"",
                "lastPrice" to "\"72000\"",
                "currency" to "\"KRW\"",
            ),
            overrides,
        )

        private fun candleItem(vararg overrides: Pair<String, String?>) = jsonObject(
            linkedMapOf(
                "timestamp" to "\"2026-03-25T00:00:00+09:00\"",
                "openPrice" to "\"1\"",
                "highPrice" to "\"2\"",
                "lowPrice" to "\"1\"",
                "closePrice" to "\"2\"",
                "volume" to "\"3\"",
                "currency" to "\"KRW\"",
            ),
            overrides,
        )

        private fun line(vararg overrides: Pair<String, String?>) = jsonObject(
            linkedMapOf("price" to "\"1\"", "volume" to "\"1\""),
            overrides,
        )

        private fun book(vararg overrides: Pair<String, String?>) = jsonObject(
            linkedMapOf(
                "timestamp" to "\"2026-03-25T09:30:00.123+09:00\"",
                "currency" to "\"KRW\"",
                "asks" to "[]",
                "bids" to "[]",
            ),
            overrides,
        )

        private fun page(vararg overrides: Pair<String, String?>) = jsonObject(
            linkedMapOf("candles" to "[]", "nextBefore" to "null"),
            overrides,
        )

        // 봉 하나만 든 result 본문
        private fun withCandles(candle: String) = "{\"result\":${page("candles" to "[$candle]")}}"

        private fun case(label: String, body: String): Arguments = Arguments.of(label, body)

        @JvmStatic
        fun invalidPriceBodies(): List<Arguments> = listOf(
            case("result 누락", "{}"),
            case("result 가 객체", "{\"result\":${priceItem()}}"),
            case("원소 null", "{\"result\":[null]}"),
            case("symbol 누락", "{\"result\":[${priceItem("symbol" to null)}]}"),
            case("lastPrice 누락", "{\"result\":[${priceItem("lastPrice" to null)}]}"),
            case("currency 누락", "{\"result\":[${priceItem("currency" to null)}]}"),
            case("lastPrice null", "{\"result\":[${priceItem("lastPrice" to "null")}]}"),
            case("lastPrice 문자", "{\"result\":[${priceItem("lastPrice" to "\"abc\"")}]}"),
            case("lastPrice 빈 문자열", "{\"result\":[${priceItem("lastPrice" to "\"\"")}]}"),
            case("timestamp 형식 오류", "{\"result\":[${priceItem("timestamp" to "\"yesterday\"")}]}"),
            case("timestamp 오프셋 없음", "{\"result\":[${priceItem("timestamp" to "\"2026-03-25T09:30:00\"")}]}"),
            case("timestamp 빈 문자열", "{\"result\":[${priceItem("timestamp" to "\"\"")}]}"),
        )

        @JvmStatic
        fun invalidOrderbookBodies(): List<Arguments> = listOf(
            case("result 누락", "{}"),
            case("result 가 배열", "{\"result\":[]}"),
            case("currency 누락", "{\"result\":${book("currency" to null)}}"),
            case("asks 누락", "{\"result\":${book("asks" to null)}}"),
            case("bids 누락", "{\"result\":${book("bids" to null)}}"),
            case("asks null", "{\"result\":${book("asks" to "null")}}"),
            case("호가 원소 null", "{\"result\":${book("asks" to "[null]")}}"),
            case("price 누락", "{\"result\":${book("asks" to "[${line("price" to null)}]")}}"),
            case("volume 누락", "{\"result\":${book("bids" to "[${line("volume" to null)}]")}}"),
            case("price 문자", "{\"result\":${book("asks" to "[${line("price" to "\"x\"")}]")}}"),
            case("volume 빈 문자열", "{\"result\":${book("bids" to "[${line("volume" to "\"\"")}]")}}"),
            case("timestamp 형식 오류", "{\"result\":${book("timestamp" to "\"x\"")}}"),
        )

        @JvmStatic
        fun invalidCandleBodies(): List<Arguments> = listOf(
            case("result 누락", "{}"),
            case("candles 누락", "{\"result\":${page("candles" to null)}}"),
            case("candles null", "{\"result\":${page("candles" to "null")}}"),
            case("봉 null", "{\"result\":${page("candles" to "[null]")}}"),
            case("가격 문자", withCandles(candleItem("openPrice" to "\"x\""))),
            case("가격 빈 문자열", withCandles(candleItem("openPrice" to "\"\""))),
            case("봉 timestamp 형식 오류", withCandles(candleItem("timestamp" to "\"2026-03-25\""))),
            case("nextBefore 형식 오류", "{\"result\":${page("nextBefore" to "\"later\"")}}"),
            case("nextBefore 오프셋 없음", "{\"result\":${page("nextBefore" to "\"2026-03-25T00:00:00\"")}}"),
        )
    }
}
