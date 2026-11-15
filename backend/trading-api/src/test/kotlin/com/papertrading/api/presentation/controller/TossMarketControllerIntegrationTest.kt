package com.papertrading.api.presentation.controller

import com.papertrading.api.application.toss.port.TossCandle
import com.papertrading.api.application.toss.port.TossCandleInterval
import com.papertrading.api.application.toss.port.TossCandlePage
import com.papertrading.api.application.toss.port.TossCandleQuery
import com.papertrading.api.application.toss.port.TossMarketDataPort
import com.papertrading.api.application.toss.port.TossOrderbook
import com.papertrading.api.application.toss.port.TossOrderbookEntry
import com.papertrading.api.application.toss.port.TossPrice
import com.papertrading.api.common.exception.TossIntegrationDisabledException
import com.papertrading.api.common.exception.TossRequestRejectedException
import com.papertrading.api.common.exception.TossResponseInvalidException
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal

// GET /api/toss/market/prices·orderbook·candles: 가격과 수량은 JSON 문자열(FLAG-10), 시각은 토스 원문 문자열 (D9, 사람 결정 2026-11-15)
// 유스케이스(TossMarketQueryService)는 실제 빈을 쓰고 HTTP 경계인 TossMarketDataPort 만 가짜로 바꿔, 입력 검증 400 이 실제 검증을 지남 (S7-04)
// devkit:test-change S7-04 TT7 서비스 목(@MockitoBean)을 실제 서비스 + 가짜 포트로 바꿈
// HTTP 응답 기대(상태·code·문자열 숫자·원문 시각)는 그대로이고 서비스에 넘긴 인자 단언은 포트가 받은 인자 단언으로 옮김
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class TossMarketControllerIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc

    @Autowired lateinit var fakePort: FakeTossMarketDataPort

    // 시험이 정한 결과를 돌려주고 받은 인자를 기록하는 가짜 포트, 실패를 정하면 호출 때 던짐
    class FakeTossMarketDataPort : TossMarketDataPort {
        var prices: List<TossPrice> = emptyList()
        var orderbook: TossOrderbook = TossOrderbook(null, "KRW", emptyList(), emptyList())
        var candlePage: TossCandlePage = TossCandlePage(emptyList(), null)
        var failure: RuntimeException? = null
        val priceCalls = mutableListOf<List<String>>()
        val orderbookCalls = mutableListOf<String>()
        val candleCalls = mutableListOf<TossCandleQuery>()

        fun reset() {
            prices = emptyList()
            orderbook = TossOrderbook(null, "KRW", emptyList(), emptyList())
            candlePage = TossCandlePage(emptyList(), null)
            failure = null
            priceCalls.clear()
            orderbookCalls.clear()
            candleCalls.clear()
        }

        override fun fetchPrices(symbols: List<String>): List<TossPrice> {
            priceCalls += symbols
            failure?.let { throw it }
            return prices
        }

        override fun fetchOrderbook(symbol: String): TossOrderbook {
            orderbookCalls += symbol
            failure?.let { throw it }
            return orderbook
        }

        override fun fetchCandles(query: TossCandleQuery): TossCandlePage {
            candleCalls += query
            failure?.let { throw it }
            return candlePage
        }
    }

    @TestConfiguration
    class FakePortConfig {
        @Bean
        @Primary
        fun fakeTossMarketDataPort(): FakeTossMarketDataPort = FakeTossMarketDataPort()
    }

    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @Container
        @ServiceConnection(name = "redis")
        val redis = GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379)
    }

    @BeforeEach
    fun resetFake() {
        fakePort.reset()
    }

    private fun candle(timestamp: String, open: String, currency: String) = TossCandle(
        timestamp = timestamp,
        openPrice = BigDecimal(open),
        highPrice = BigDecimal("186.20"),
        lowPrice = BigDecimal("183.90"),
        closePrice = BigDecimal("185.70"),
        volume = BigDecimal("48210000"),
        currency = currency,
    )

    @Test
    fun `현재가는 200 이고 가격이 JSON 문자열이며 시각은 토스 원문 그대로이다`() {
        fakePort.prices = listOf(
            TossPrice("005930", "2026-03-25T09:30:00.123+09:00", BigDecimal("72000"), "KRW"),
            TossPrice("AAPL", null, BigDecimal("185.70"), "USD"),
        )

        mockMvc.get("/api/toss/market/prices") { param("symbols", "005930,AAPL") }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[0].symbol") { value("005930") }
            jsonPath("$[0].timestamp") { value("2026-03-25T09:30:00.123+09:00") }
            jsonPath("$[0].lastPrice") { value("72000") }
            jsonPath("$[0].lastPrice") { isString() }
            jsonPath("$[0].currency") { value("KRW") }
            jsonPath("$[1].symbol") { value("AAPL") }
            jsonPath("$[1].timestamp") { value(null as Any?) }
            jsonPath("$[1].lastPrice") { value("185.70") }
        }
        assertEquals(listOf(listOf("005930", "AAPL")), fakePort.priceCalls, "서비스가 쉼표 목록으로 쪼개 포트에 넘김")
    }

    @Test
    fun `현재가 가격은 지수 표기 없이 자릿수 그대로 문자열이다`() {
        fakePort.prices = listOf(TossPrice("X", null, BigDecimal("123456789012345.123456"), "USD"))

        mockMvc.get("/api/toss/market/prices") { param("symbols", "X") }.andExpect {
            status { isOk() }
            jsonPath("$[0].lastPrice") { value("123456789012345.123456") }
        }
    }

    @Test
    fun `빈 현재가 결과는 빈 배열이다`() {
        mockMvc.get("/api/toss/market/prices") { param("symbols", "A") }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(0) }
        }
    }

    @Test
    fun `호가는 200 이고 가격과 수량이 JSON 문자열이며 순서 그대로이다`() {
        fakePort.orderbook = TossOrderbook(
            timestamp = "2026-03-25T22:30:00.456+09:00",
            currency = "USD",
            asks = listOf(
                TossOrderbookEntry(BigDecimal("185.75"), BigDecimal("250")),
                TossOrderbookEntry(BigDecimal("185.70"), BigDecimal("410")),
            ),
            bids = listOf(TossOrderbookEntry(BigDecimal("185.65"), BigDecimal("180"))),
        )

        mockMvc.get("/api/toss/market/orderbook") { param("symbol", "AAPL") }.andExpect {
            status { isOk() }
            jsonPath("$.timestamp") { value("2026-03-25T22:30:00.456+09:00") }
            jsonPath("$.currency") { value("USD") }
            jsonPath("$.asks[0].price") { value("185.75") }
            jsonPath("$.asks[0].price") { isString() }
            jsonPath("$.asks[0].volume") { value("250") }
            jsonPath("$.asks[1].price") { value("185.70") }
            jsonPath("$.bids[0].price") { value("185.65") }
            jsonPath("$.bids[0].volume") { value("180") }
        }
        assertEquals(listOf("AAPL"), fakePort.orderbookCalls)
    }

    @Test
    fun `빈 호가는 빈 배열과 null 시각이다`() {
        mockMvc.get("/api/toss/market/orderbook") { param("symbol", "A") }.andExpect {
            status { isOk() }
            jsonPath("$.timestamp") { value(null as Any?) }
            jsonPath("$.asks.length()") { value(0) }
            jsonPath("$.bids.length()") { value(0) }
        }
    }

    @Test
    fun `캔들은 200 이고 숫자가 JSON 문자열이며 미국 일봉 시각의 -04 00 오프셋이 그대로이다`() {
        fakePort.candlePage = TossCandlePage(
            candles = listOf(
                candle("2026-03-25T00:00:00-04:00", "184.10", "USD"),
                candle("2026-03-24T00:00:00-04:00", "183.00", "USD"),
            ),
            nextBefore = "2026-03-24T00:00:00-04:00",
        )

        mockMvc.get("/api/toss/market/candles") {
            param("symbol", "AAPL")
            param("interval", "1d")
            param("count", "2")
            param("before", "2026-03-25T00:00:00-04:00")
            param("adjusted", "true")
        }.andExpect {
            status { isOk() }
            jsonPath("$.candles.length()") { value(2) }
            jsonPath("$.candles[0].timestamp") { value("2026-03-25T00:00:00-04:00") }
            jsonPath("$.candles[1].timestamp") { value("2026-03-24T00:00:00-04:00") }
            jsonPath("$.candles[0].openPrice") { value("184.10") }
            jsonPath("$.candles[0].openPrice") { isString() }
            jsonPath("$.candles[0].highPrice") { value("186.20") }
            jsonPath("$.candles[0].lowPrice") { value("183.90") }
            jsonPath("$.candles[0].closePrice") { value("185.70") }
            jsonPath("$.candles[0].volume") { value("48210000") }
            jsonPath("$.candles[0].volume") { isString() }
            jsonPath("$.candles[0].currency") { value("USD") }
            jsonPath("$.nextBefore") { value("2026-03-24T00:00:00-04:00") }
        }
        assertEquals(
            listOf(TossCandleQuery("AAPL", TossCandleInterval.ONE_DAY, 2, "2026-03-25T00:00:00-04:00", true)),
            fakePort.candleCalls,
            "서비스가 count·adjusted 를 변환하고 before 는 검증만 한 원문으로 포트에 넘김",
        )
    }

    @Test
    fun `before 의 + 는 디코딩된 값 그대로 서비스에 넘기고 마지막 쪽 nextBefore 는 null 이다`() {
        mockMvc.get("/api/toss/market/candles") {
            param("symbol", "005930")
            param("interval", "1m")
            param("before", "2026-03-25T09:00:00+09:00")
        }.andExpect {
            status { isOk() }
            jsonPath("$.candles.length()") { value(0) }
            jsonPath("$.nextBefore") { value(null as Any?) }
        }
        assertEquals(
            listOf(TossCandleQuery("005930", TossCandleInterval.ONE_MINUTE, null, "2026-03-25T09:00:00+09:00", null)),
            fakePort.candleCalls,
        )
    }

    @Test
    fun `입력 검증 실패는 400 INVALID_MARKET_QUERY 이고 문구에 입력값이 없으며 토스 포트를 부르지 않는다`() {
        mockMvc.get("/api/toss/market/prices") { param("symbols", "SECRET-INPUT#") }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_MARKET_QUERY") }
            content { string(not(containsString("SECRET"))) }
        }

        assertTrue(fakePort.priceCalls.isEmpty(), "검증에서 막혀 포트까지 가지 않음: ${fakePort.priceCalls}")
    }

    @Test
    fun `필수 파라미터가 없으면 실제 서비스 검증이 400 으로 거부하고 포트를 부르지 않는다`() {
        mockMvc.get("/api/toss/market/prices").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_MARKET_QUERY") }
        }
        mockMvc.get("/api/toss/market/orderbook").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_MARKET_QUERY") }
        }
        mockMvc.get("/api/toss/market/candles").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_MARKET_QUERY") }
        }

        assertTrue(fakePort.priceCalls.isEmpty() && fakePort.orderbookCalls.isEmpty() && fakePort.candleCalls.isEmpty())
    }

    @ParameterizedTest(name = "캔들 {0}={1} 은 400 INVALID_MARKET_QUERY")
    @CsvSource(
        "interval, 5m",
        "count, 0",
        "count, 201",
        "count, abc",
        "before, 2026-03-25T09:00:00",
        "before, yesterday",
        "adjusted, TRUE",
        "adjusted, yes",
        "symbol, A B",
    )
    fun `캔들의 잘못된 파라미터는 실제 서비스 검증으로 400 이다`(name: String, value: String) {
        val valid = mapOf("symbol" to "AAPL", "interval" to "1d")

        mockMvc.get("/api/toss/market/candles") {
            (valid + (name to value)).forEach { (key, v) -> param(key, v) }
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_MARKET_QUERY") }
        }

        assertTrue(fakePort.candleCalls.isEmpty(), "$name=$value: ${fakePort.candleCalls}")
    }

    @Test
    fun `토스 실패는 범주별 상태와 code 로 매핑된다`() {
        fakePort.failure = TossRequestRejectedException(404)
        mockMvc.get("/api/toss/market/orderbook") { param("symbol", "ZZZZ") }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("TOSS_REQUEST_REJECTED") }
        }

        fakePort.failure = TossResponseInvalidException("필드 경로=result")
        mockMvc.get("/api/toss/market/prices") { param("symbols", "A") }.andExpect {
            status { isServiceUnavailable() }
            jsonPath("$.code") { value("TOSS_RESPONSE_INVALID") }
        }

        fakePort.failure = TossIntegrationDisabledException(listOf("TOSS_CLIENT_ID"))
        mockMvc.get("/api/toss/market/candles") {
            param("symbol", "A")
            param("interval", "1d")
        }.andExpect {
            status { isInternalServerError() }
            jsonPath("$.code") { value("TOSS_INTEGRATION_DISABLED") }
        }
    }
}
