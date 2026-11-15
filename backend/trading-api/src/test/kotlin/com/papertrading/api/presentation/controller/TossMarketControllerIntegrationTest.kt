package com.papertrading.api.presentation.controller

import com.papertrading.api.application.toss.TossMarketQueryService
import com.papertrading.api.application.toss.port.TossCandle
import com.papertrading.api.application.toss.port.TossCandlePage
import com.papertrading.api.application.toss.port.TossOrderbook
import com.papertrading.api.application.toss.port.TossOrderbookEntry
import com.papertrading.api.application.toss.port.TossPrice
import com.papertrading.api.common.exception.InvalidMarketQueryException
import com.papertrading.api.common.exception.TossIntegrationDisabledException
import com.papertrading.api.common.exception.TossRequestRejectedException
import com.papertrading.api.common.exception.TossResponseInvalidException
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.verify
import org.mockito.BDDMockito.willThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal

// GET /api/toss/market/prices·orderbook·candles: 가격과 수량은 JSON 문자열(FLAG-10), 시각은 토스 원문 문자열 (D9, 사람 결정 2026-11-15)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class TossMarketControllerIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc

    @MockitoBean lateinit var tossMarketQueryService: TossMarketQueryService

    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @Container
        @ServiceConnection(name = "redis")
        val redis = GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379)
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
        given(tossMarketQueryService.getPrices("005930,AAPL")).willReturn(
            listOf(
                TossPrice("005930", "2026-03-25T09:30:00.123+09:00", BigDecimal("72000"), "KRW"),
                TossPrice("AAPL", null, BigDecimal("185.70"), "USD"),
            ),
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
    }

    @Test
    fun `현재가 가격은 지수 표기 없이 자릿수 그대로 문자열이다`() {
        given(tossMarketQueryService.getPrices("X")).willReturn(
            listOf(TossPrice("X", null, BigDecimal("123456789012345.123456"), "USD")),
        )

        mockMvc.get("/api/toss/market/prices") { param("symbols", "X") }.andExpect {
            status { isOk() }
            jsonPath("$[0].lastPrice") { value("123456789012345.123456") }
        }
    }

    @Test
    fun `빈 현재가 결과는 빈 배열이다`() {
        given(tossMarketQueryService.getPrices("A")).willReturn(emptyList())

        mockMvc.get("/api/toss/market/prices") { param("symbols", "A") }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(0) }
        }
    }

    @Test
    fun `호가는 200 이고 가격과 수량이 JSON 문자열이며 순서 그대로이다`() {
        given(tossMarketQueryService.getOrderbook("AAPL")).willReturn(
            TossOrderbook(
                timestamp = "2026-03-25T22:30:00.456+09:00",
                currency = "USD",
                asks = listOf(
                    TossOrderbookEntry(BigDecimal("185.75"), BigDecimal("250")),
                    TossOrderbookEntry(BigDecimal("185.70"), BigDecimal("410")),
                ),
                bids = listOf(TossOrderbookEntry(BigDecimal("185.65"), BigDecimal("180"))),
            ),
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
    }

    @Test
    fun `빈 호가는 빈 배열과 null 시각이다`() {
        given(tossMarketQueryService.getOrderbook("A")).willReturn(TossOrderbook(null, "KRW", emptyList(), emptyList()))

        mockMvc.get("/api/toss/market/orderbook") { param("symbol", "A") }.andExpect {
            status { isOk() }
            jsonPath("$.timestamp") { value(null as Any?) }
            jsonPath("$.asks.length()") { value(0) }
            jsonPath("$.bids.length()") { value(0) }
        }
    }

    @Test
    fun `캔들은 200 이고 숫자가 JSON 문자열이며 미국 일봉 시각의 -04 00 오프셋이 그대로이다`() {
        given(
            tossMarketQueryService.getCandles("AAPL", "1d", "2", "2026-03-25T00:00:00-04:00", "true"),
        ).willReturn(
            TossCandlePage(
                candles = listOf(
                    candle("2026-03-25T00:00:00-04:00", "184.10", "USD"),
                    candle("2026-03-24T00:00:00-04:00", "183.00", "USD"),
                ),
                nextBefore = "2026-03-24T00:00:00-04:00",
            ),
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
    }

    @Test
    fun `before 의 + 는 디코딩된 값 그대로 서비스에 넘기고 마지막 쪽 nextBefore 는 null 이다`() {
        given(
            tossMarketQueryService.getCandles("005930", "1m", null, "2026-03-25T09:00:00+09:00", null),
        ).willReturn(TossCandlePage(emptyList(), null))

        mockMvc.get("/api/toss/market/candles") {
            param("symbol", "005930")
            param("interval", "1m")
            param("before", "2026-03-25T09:00:00+09:00")
        }.andExpect {
            status { isOk() }
            jsonPath("$.candles.length()") { value(0) }
            jsonPath("$.nextBefore") { value(null as Any?) }
        }
        verify(tossMarketQueryService).getCandles("005930", "1m", null, "2026-03-25T09:00:00+09:00", null)
    }

    @Test
    fun `입력 검증 실패는 400 INVALID_MARKET_QUERY 이고 문구에 입력값이 없다`() {
        willThrow(InvalidMarketQueryException("symbols", "원소는 영문 대소문자 숫자 점 하이픈만"))
            .given(tossMarketQueryService)
            .getPrices("SECRET-INPUT#")

        mockMvc.get("/api/toss/market/prices") { param("symbols", "SECRET-INPUT#") }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_MARKET_QUERY") }
            content { string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SECRET"))) }
        }
    }

    @Test
    fun `필수 파라미터가 없으면 null 로 서비스에 넘겨 서비스가 400 으로 거부한다`() {
        willThrow(InvalidMarketQueryException("symbol", "필수"))
            .given(tossMarketQueryService)
            .getOrderbook(null)
        willThrow(InvalidMarketQueryException("interval", "1m 또는 1d"))
            .given(tossMarketQueryService)
            .getCandles(null, null, null, null, null)

        mockMvc.get("/api/toss/market/orderbook").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_MARKET_QUERY") }
        }
        mockMvc.get("/api/toss/market/candles").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_MARKET_QUERY") }
        }
    }

    @Test
    fun `토스 실패는 범주별 상태와 code 로 매핑된다`() {
        willThrow(TossRequestRejectedException(404)).given(tossMarketQueryService).getOrderbook("ZZZZ")
        mockMvc.get("/api/toss/market/orderbook") { param("symbol", "ZZZZ") }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("TOSS_REQUEST_REJECTED") }
        }

        willThrow(TossResponseInvalidException("필드 경로=result")).given(tossMarketQueryService).getPrices("A")
        mockMvc.get("/api/toss/market/prices") { param("symbols", "A") }.andExpect {
            status { isServiceUnavailable() }
            jsonPath("$.code") { value("TOSS_RESPONSE_INVALID") }
        }

        willThrow(TossIntegrationDisabledException(listOf("TOSS_CLIENT_ID")))
            .given(tossMarketQueryService)
            .getCandles("A", "1d", null, null, null)
        mockMvc.get("/api/toss/market/candles") {
            param("symbol", "A")
            param("interval", "1d")
        }.andExpect {
            status { isInternalServerError() }
            jsonPath("$.code") { value("TOSS_INTEGRATION_DISABLED") }
        }
    }
}
