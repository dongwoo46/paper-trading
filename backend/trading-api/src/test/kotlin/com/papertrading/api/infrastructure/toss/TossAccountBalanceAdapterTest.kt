package com.papertrading.api.infrastructure.toss

import com.papertrading.api.application.account.port.BrokerAccountRef
import com.papertrading.api.application.account.port.BrokerBalanceSnapshot
import com.papertrading.api.common.exception.TossIntegrationDisabledException
import com.papertrading.api.common.exception.TossResponseInvalidException
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.infrastructure.toss.TossFixtures.reply
import com.papertrading.api.infrastructure.toss.TossFixtures.replyRaw
import com.papertrading.api.infrastructure.toss.TossFixtures.tooManyRequests
import com.papertrading.api.support.LogCapture
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.aggregator.ArgumentsAccessor
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.ResponseActions
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Base64

// 토스 잔고 어댑터: holdings 1회 + buying-power KRW·USD 2회를 공통 포트 모양으로 (D7, D13, D14, 필드표 부록 A)
class TossAccountBalanceAdapterTest {

    private val restTemplate = RestTemplate()
    private val server = MockRestServiceServer.bindTo(restTemplate).build()
    private val tokenManager = mockk<TossTokenManager>()
    private val limiter = mockk<TossRateLimiter>(relaxed = true)

    // 시험이 정한 임의 값, 토스 정책 수치가 아님
    private val properties = TossProperties().apply {
        baseUrl = "https://toss.example.invalid"
        clientId = "fake-client-id"
        clientSecret = "fake-client-secret"
        tokenEncryptionKey = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })
        rateLimits = TossRateLimitGroup.entries.associateWith { 1_000 }
    }
    private val fixedClock = Clock.fixed(Instant.parse("2026-11-15T01:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val client = TossApiClient(properties, tokenManager, limiter, restTemplate)
    private val adapter = TossAccountBalanceAdapter(client, fixedClock)

    private val accountSeq = "1234567890"
    private val account = BrokerAccountRef(7L, TradingMode.TOSS_LIVE, accountSeq)
    private val base = "https://toss.example.invalid"
    private val mixed = TossFixtures.read("success-holdings-mixed.json")
    private val krwOnly = TossFixtures.read("success-holdings-krw-only.json")

    private fun expectGet(path: String): ResponseActions = server.expect(requestTo("$base$path"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer fake-token"))
        .andExpect(header("X-Tossinvest-Account", accountSeq))

    private fun expectHoldings(body: String) = expectGet("/api/v1/holdings").andRespond(replyRaw(HttpStatus.OK, body))

    private fun expectBuyingPower(currency: String, fixture: String) {
        expectGet("/api/v1/buying-power?currency=$currency").andRespond(reply(HttpStatus.OK, fixture))
    }

    private fun expectAll(holdings: String = mixed) {
        every { tokenManager.getToken() } returns "fake-token"
        expectHoldings(holdings)
        expectBuyingPower("KRW", "success-buying-power-krw.json")
        expectBuyingPower("USD", "success-buying-power-usd.json")
    }

    private fun fetch(holdings: String = mixed): BrokerBalanceSnapshot {
        expectAll(holdings)
        return adapter.fetchBalance(account)
    }

    private fun assertDecimal(expected: String, actual: BigDecimal?, label: String) {
        val value = requireNotNull(actual) { "$label 가 null" }
        assertEquals(0, BigDecimal(expected).compareTo(value), "$label 기대=$expected 실제=$value")
        assertEquals(expected, value.toPlainString(), "$label 표기")
    }

    @Test
    fun `정상 KRW와 USD 응답을 통화별 합계 두 행과 종목으로 매핑하고 USD가 KRW 합계에 섞이지 않는다`() {
        val snapshot = fetch()

        assertEquals(listOf("KRW", "USD"), snapshot.currencyBalances.map { it.currency })
        val (krw, usd) = snapshot.currencyBalances
        assertNull(krw.cashBalance)
        assertNull(usd.cashBalance)
        assertDecimal("5000000", krw.cashBuyingPower, "KRW cashBuyingPower")
        assertDecimal("7200000", krw.marketValue, "KRW marketValue")
        assertDecimal("700000", krw.unrealizedPnl, "KRW unrealizedPnl")
        assertDecimal("10.77", krw.returnRate, "KRW returnRate")
        assertDecimal("3500.5", usd.cashBuyingPower, "USD cashBuyingPower")
        assertDecimal("1785", usd.marketValue, "USD marketValue")
        assertDecimal("232", usd.unrealizedPnl, "USD unrealizedPnl")
        assertDecimal("14.94", usd.returnRate, "USD returnRate")
        server.verify()
    }

    @Test
    fun `종목은 심볼 통화 수량 가격 평가액 손익을 옮기고 수익률은 비율에 100을 곱한 퍼센트이며 반올림하지 않는다`() {
        val positions = fetch().positions

        assertEquals(listOf("005930", "AAPL"), positions.map { it.ticker })
        val samsung = positions[0]
        assertEquals("KRW", samsung.currency)
        assertDecimal("100", samsung.quantity, "quantity")
        assertDecimal("65000", samsung.avgPrice, "avgPrice")
        assertDecimal("72000", samsung.currentPrice, "currentPrice")
        assertDecimal("7200000", samsung.marketValue, "marketValue")
        assertDecimal("700000", samsung.unrealizedPnl, "unrealizedPnl")
        assertDecimal("10.77", samsung.returnRate, "returnRate")
        val apple = positions[1]
        assertEquals("USD", apple.currency)
        assertDecimal("178.5", apple.currentPrice, "AAPL currentPrice")
        assertDecimal("14.94", apple.returnRate, "AAPL returnRate")
    }

    @Test
    fun `종목 수익률은 소수 비율 자릿수를 그대로 퍼센트로 옮겨 반올림하지 않는다`() {
        val body = krwOnly.replace(
            """"rate":"0.1077","rateAfterCost":"0.0846"}""",
            """"rate":"0.123456789","rateAfterCost":"0.0846"}""",
        )

        assertDecimal("12.3456789", fetch(body).positions.single().returnRate, "returnRate")
    }

    @Test
    fun `조회 시각은 시간대를 붙이지 않은 조회 시각이다`() {
        assertEquals(LocalDateTime.of(2026, 11, 15, 10, 0), fetch().asOf)
    }

    @Test
    fun `holdings 와 buying-power 요청마다 accountSeq 를 헤더에 싣고 한도 그룹은 ASSET 1회 ORDER_INFO 2회이다`() {
        fetch()

        server.verify()
        verifyOrder {
            limiter.acquire(TossRateLimitGroup.ASSET)
            limiter.acquire(TossRateLimitGroup.ORDER_INFO)
            limiter.acquire(TossRateLimitGroup.ORDER_INFO)
        }
        verify(exactly = 3) { limiter.acquire(any()) }
    }

    @Test
    fun `usd 가 null 이면 USD 행의 평가액 손익 수익률만 null 이고 매수 가능 금액은 buying-power 값이다`() {
        val (krw, usd) = fetch(krwOnly).currencyBalances

        assertDecimal("7200000", krw.marketValue, "KRW marketValue")
        assertNull(usd.marketValue)
        assertNull(usd.unrealizedPnl)
        assertNull(usd.returnRate)
        assertDecimal("3500.5", usd.cashBuyingPower, "USD cashBuyingPower")
    }

    @Test
    fun `usd 항목 하나만 null 이어도 그 칸만 null 이고 0 으로 채우지 않는다`() {
        val body = mixed.replace(
            """"amount":{"krw":"700000","usd":"232"}""",
            """"amount":{"krw":"700000","usd":null}""",
        )

        val usd = fetch(body).currencyBalances.last()

        assertDecimal("1785", usd.marketValue, "marketValue")
        assertNull(usd.unrealizedPnl)
        assertNull(usd.returnRate)
    }

    @Test
    fun `보유 종목이 없으면 종목은 빈 목록이고 KRW 수익률은 원금 0 이라 null 이다`() {
        val snapshot = fetch(TossFixtures.read("success-holdings-empty.json"))

        assertTrue(snapshot.positions.isEmpty())
        val krw = snapshot.currencyBalances.first()
        assertDecimal("0", krw.marketValue, "KRW marketValue")
        assertDecimal("0", krw.unrealizedPnl, "KRW unrealizedPnl")
        assertNull(krw.returnRate)
        assertDecimal("5000000", krw.cashBuyingPower, "KRW cashBuyingPower")
    }

    @Test
    fun `원금이 0 이면 그 통화 수익률은 null 이고 평가액과 손익은 그대로이다`() {
        val body = mixed.replace(
            """"totalPurchaseAmount":{"krw":"6500000","usd":"1553"}""",
            """"totalPurchaseAmount":{"krw":"6500000","usd":"0"}""",
        )

        val usd = fetch(body).currencyBalances.last()

        assertNull(usd.returnRate)
        assertDecimal("232", usd.unrealizedPnl, "unrealizedPnl")
    }

    @ParameterizedTest(name = "손익 {0} 원금 {1} 이면 수익률 {2}")
    @CsvSource("700000, 6500000, 10.77", "-1, 3, -33.33", "2, 3, 66.67", "1, 8, 12.50", "0, 6500000, 0.00")
    fun `통화별 수익률은 손익을 원금으로 나눠 scale 6 에서 반올림하고 100 을 곱해 scale 2 로 맞춘다`(
        pnl: String,
        principal: String,
        expected: String,
    ) {
        val body = mixed
            .replace(""""amount":{"krw":"700000","usd":"232"}""", """"amount":{"krw":"$pnl","usd":"232"}""")
            .replace(""""totalPurchaseAmount":{"krw":"6500000"""", """"totalPurchaseAmount":{"krw":"$principal"""")

        assertDecimal(expected, fetch(body).currencyBalances.first().returnRate, "returnRate")
    }

    @Test
    fun `토스 합계 rate 는 원화 환산 전체 기준이라 쓰지 않는다`() {
        val body = mixed.replace(""""rate":"0.1179"""", """"rate":"9.9999"""")

        assertDecimal("10.77", fetch(body).currencyBalances.first().returnRate, "returnRate")
    }

    @Test
    fun `긴 자릿수 정수부 15자리와 소수 6자리를 문자열과 JSON 숫자 모두 잃지 않고 옮긴다`() {
        val long = "123456789012345.123456"
        val body = krwOnly
            .replace(""""quantity":"100"""", """"quantity":"$long"""")
            .replace(""""lastPrice":"72000"""", """"lastPrice":$long""")
            .replace(""""averagePurchasePrice":"65000"""", """"averagePurchasePrice":"$long"""")
            .replace(
                """"marketValue":{"purchaseAmount":"6500000","amount":"7200000"""",
                """"marketValue":{"purchaseAmount":"6500000","amount":"$long"""",
            )
            .replace(""""amount":{"krw":"7200000","usd":null}""", """"amount":{"krw":"$long","usd":null}""")

        val snapshot = fetch(body)

        val position = snapshot.positions.single()
        assertDecimal(long, position.quantity, "quantity")
        assertDecimal(long, position.currentPrice, "currentPrice")
        assertDecimal(long, position.avgPrice, "avgPrice")
        assertDecimal(long, position.marketValue, "position marketValue")
        assertDecimal(long, snapshot.currencyBalances.first().marketValue, "KRW marketValue")
    }

    @Test
    fun `매수 가능 금액도 긴 자릿수를 잃지 않는다`() {
        every { tokenManager.getToken() } returns "fake-token"
        expectHoldings(mixed)
        val long = "123456789012345.123456"
        expectGet("/api/v1/buying-power?currency=KRW")
            .andRespond(replyRaw(HttpStatus.OK, """{"result":{"currency":"KRW","cashBuyingPower":"$long"}}"""))
        expectBuyingPower("USD", "success-buying-power-usd.json")

        assertDecimal(long, adapter.fetchBalance(account).currencyBalances.first().cashBuyingPower, "cashBuyingPower")
    }

    @Test
    fun `모르는 통화와 시장 국가 종목은 그대로 싣고 통화별 합계 행은 KRW USD 두 개로 고정이다`() {
        val body = krwOnly
            .replace(""""marketCountry":"KR","currency":"KRW"""", """"marketCountry":"JP","currency":"JPY"""")

        val snapshot = fetch(body)

        assertEquals("JPY", snapshot.positions.single().currency)
        assertEquals(listOf("KRW", "USD"), snapshot.currencyBalances.map { it.currency })
        assertDecimal("7200000", snapshot.currencyBalances.first().marketValue, "KRW marketValue")
    }

    // 명세 required 필드를 하나씩 뺀 본문 (부록 A), 뺄 문자열이 fixture 에 실제로 있는지도 확인
    @ParameterizedTest(name = "필수 필드 {0} 가 없으면 TOSS_RESPONSE_INVALID")
    @CsvSource(
        delimiter = '|',
        value = [
            "totalPurchaseAmount|\"totalPurchaseAmount\":{\"krw\":\"6500000\",\"usd\":\"1553\"},",
            "totalPurchaseAmount.krw|\"totalPurchaseAmount\":{\"krw\":\"6500000\",|\"totalPurchaseAmount\":{",
            "marketValue.amount.krw|\"amount\":{\"krw\":\"7200000\",|\"amount\":{",
            "profitLoss.amount.krw|\"amount\":{\"krw\":\"700000\",|\"amount\":{",
            "profitLoss.rate|\"rate\":\"0.1179\",",
            "dailyProfitLoss|\"dailyProfitLoss\":{\"amount\":{\"krw\":\"100000\",\"usd\":\"25\"},\"rate\":\"0.0141\"},",
            "items|\"items\":[|\"itemz\":[",
            "items.symbol|\"symbol\":\"005930\",",
            "items.name|\"name\":\"삼성전자\",",
            "items.marketCountry|\"marketCountry\":\"KR\",",
            "items.currency|\"currency\":\"KRW\",",
            "items.quantity|\"quantity\":\"100\",",
            "items.lastPrice|\"lastPrice\":\"72000\",",
            "items.averagePurchasePrice|\"averagePurchasePrice\":\"65000\",",
            "items.marketValue.amount|\"amount\":\"7200000\",",
            "items.profitLoss.amount|\"profitLoss\":{\"amount\":\"700000\",|\"profitLoss\":{",
            "items.profitLoss.rate|\"rate\":\"0.1077\",",
            "items.dailyProfitLoss|\"dailyProfitLoss\":{\"amount\":\"100000\",\"rate\":\"0.0141\"},",
            "items.cost|,\"cost\":{\"commission\":\"14400\",\"tax\":\"135600\"}",
        ],
    )
    fun `holdings 필수 필드 누락은 TOSS_RESPONSE_INVALID 이고 0 으로 채우지 않는다`(row: ArgumentsAccessor) {
        val label = row.getString(0)
        val needle = row.getString(1)
        // 셋째 칸이 없으면 그 조각을 지움
        val replacement = if (row.size() > 2) row.getString(2) else ""
        assertTrue(mixed.contains(needle), "fixture 에 없는 조각: $needle")
        every { tokenManager.getToken() } returns "fake-token"
        expectHoldings(mixed.replaceFirst(needle, replacement))

        val error = assertThrows<TossResponseInvalidException>(label) { adapter.fetchBalance(account) }

        assertEquals("TOSS_RESPONSE_INVALID", error.errorCode)
        verify(exactly = 1) { limiter.acquire(any()) }
    }

    @ParameterizedTest(name = "숫자 변환 실패 {0}")
    @CsvSource(
        delimiter = '|',
        value = [
            "quantity 문자|\"quantity\":\"100\"|\"quantity\":\"abc\"",
            "lastPrice null|\"lastPrice\":\"72000\"|\"lastPrice\":null",
            "averagePurchasePrice 빈 문자열|\"averagePurchasePrice\":\"65000\"|\"averagePurchasePrice\":\"\"",
            "profitLoss.rate 문자|\"rate\":\"0.1077\"|\"rate\":\"x\"",
            "합계 marketValue 문자|\"amount\":{\"krw\":\"7200000\"|\"amount\":{\"krw\":\"seven\"",
        ],
    )
    fun `숫자 변환 실패는 TOSS_RESPONSE_INVALID 이다`(label: String, needle: String, replacement: String) {
        assertTrue(mixed.contains(needle), "fixture 에 없는 조각: $needle")
        every { tokenManager.getToken() } returns "fake-token"
        expectHoldings(mixed.replaceFirst(needle, replacement))

        assertThrows<TossResponseInvalidException>(label) { adapter.fetchBalance(account) }
    }

    @Test
    fun `종목 목록 원소가 null 이면 조용히 빠지지 않고 TOSS_RESPONSE_INVALID 이다`() {
        every { tokenManager.getToken() } returns "fake-token"
        expectHoldings(TossFixtures.read("success-holdings-empty.json").replace(""""items":[]""", """"items":[null]"""))

        assertThrows<TossResponseInvalidException> { adapter.fetchBalance(account) }
    }

    @Test
    fun `buying-power 응답 통화가 요청 통화와 다르면 TOSS_RESPONSE_INVALID 이다`() {
        every { tokenManager.getToken() } returns "fake-token"
        expectHoldings(mixed)
        expectBuyingPower("KRW", "success-buying-power-usd.json")

        assertThrows<TossResponseInvalidException> { adapter.fetchBalance(account) }
    }

    @ParameterizedTest(name = "buying-power 본문 {0}")
    @CsvSource(
        delimiter = '|',
        value = [
            "cashBuyingPower 누락|{\"result\":{\"currency\":\"KRW\"}}",
            "cashBuyingPower 문자|{\"result\":{\"currency\":\"KRW\",\"cashBuyingPower\":\"abc\"}}",
            "currency 누락|{\"result\":{\"cashBuyingPower\":\"1\"}}",
            "result 누락|{}",
        ],
    )
    fun `buying-power 필수 필드 누락과 변환 실패는 TOSS_RESPONSE_INVALID 이다`(label: String, body: String) {
        every { tokenManager.getToken() } returns "fake-token"
        expectHoldings(mixed)
        expectGet("/api/v1/buying-power?currency=KRW").andRespond(replyRaw(HttpStatus.OK, body))

        assertThrows<TossResponseInvalidException>(label) { adapter.fetchBalance(account) }
    }

    @Test
    fun `재시도와 응답 불일치 경로의 로그와 예외 문구에 accountSeq 전체가 없고 끝 4자리만 남는다`() {
        LogCapture().use { logs ->
            every { tokenManager.getToken() } returns "fake-token"
            expectGet("/api/v1/holdings").andRespond(tooManyRequests("0"))
            expectHoldings(mixed)
            expectBuyingPower("KRW", "success-buying-power-krw.json")
            expectBuyingPower("USD", "success-buying-power-usd.json")
            adapter.fetchBalance(account)
            server.reset()
            expectHoldings(mixed.replaceFirst(""""quantity":"100"""", """"quantity":"abc""""))

            val error = assertThrows<TossResponseInvalidException> { adapter.fetchBalance(account) }

            // 목 라이브러리 자신의 DEBUG 로그(스텁 응답 값 출력)는 우리 코드 로그가 아니라 제외
            val lines = logs.lines().filterNot { it.startsWith("io.mockk") }
            assertTrue(lines.none { it.contains(accountSeq) }, "accountSeq 전체가 로그에 있음: $lines")
            assertTrue(lines.any { it.contains("7890") }, "끝 4자리는 남아 식별 가능해야 함")
            assertTrue(!error.message.orEmpty().contains(accountSeq), error.message)
        }
    }

    @Test
    fun `토스 설정이 비활성이면 호출 없이 TOSS_INTEGRATION_DISABLED 이다`() {
        val disabledClient = TossApiClient(TossProperties(), tokenManager, limiter, restTemplate)
        val disabled = TossAccountBalanceAdapter(disabledClient, fixedClock)

        assertThrows<TossIntegrationDisabledException> { disabled.fetchBalance(account) }

        server.verify()
    }
}
