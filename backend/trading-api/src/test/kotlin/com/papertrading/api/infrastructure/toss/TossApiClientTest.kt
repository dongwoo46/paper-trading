package com.papertrading.api.infrastructure.toss

import com.fasterxml.jackson.core.type.TypeReference
import com.papertrading.api.common.exception.ServiceUnavailableException
import com.papertrading.api.common.exception.TossAuthFailedException
import com.papertrading.api.common.exception.TossIntegrationDisabledException
import com.papertrading.api.common.exception.TossIpNotAllowedException
import com.papertrading.api.common.exception.TossMaintenanceException
import com.papertrading.api.common.exception.TossRateLimitedException
import com.papertrading.api.common.exception.TossRequestRejectedException
import com.papertrading.api.common.exception.TossResponseInvalidException
import com.papertrading.api.common.exception.TossTimeoutException
import com.papertrading.api.common.exception.TossTokenUnavailableException
import com.papertrading.api.common.exception.TossUnavailableException
import com.papertrading.api.infrastructure.toss.TossFixtures.reply
import com.papertrading.api.infrastructure.toss.TossFixtures.replyRaw
import com.papertrading.api.infrastructure.toss.TossFixtures.tooManyRequests
import com.papertrading.api.support.LogCapture
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.ResponseActions
import org.springframework.test.web.client.ResponseCreator
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.math.BigDecimal
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.Base64
import java.util.concurrent.atomic.AtomicLong

// 시험 전용 응답 형태, 실제 DTO 는 step 6~7
data class PriceSample(
    val symbol: String,
    val lastPrice: BigDecimal,
    val currency: String,
    val timestamp: String? = null,
)

// 공통 전송: 헤더, 401 재시도, 403, 429 대기, 5xx·네트워크·타임아웃, 4xx, 깨진 응답 (D12, D13, D14, D15, D16)
class TossApiClientTest {

    private val restTemplate = RestTemplate()
    private val server = MockRestServiceServer.bindTo(restTemplate).build()
    private val tokenManager = mockk<TossTokenManager>()

    private val limiterClock = AtomicLong(0)
    private val limiterSlept = mutableListOf<Long>()
    private val rateWaits = mutableListOf<Long>()

    // 시험이 정한 임의 값, 토스 정책 수치가 아님
    private val properties = TossProperties().apply {
        baseUrl = "https://toss.example.invalid"
        clientId = "fake-client-id"
        clientSecret = "fake-client-secret"
        tokenEncryptionKey = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })
        rateLimits = TossRateLimitGroup.entries.associateWith { 1_000 }
        rateLimitMaxRetries = 2
        rateLimitMaxWaitMillis = 5_000
    }
    private val limiter = TossRateLimiter(properties, limiterClock::get) { millis ->
        limiterSlept += millis
        limiterClock.addAndGet(millis)
    }
    private val client = TossApiClient(properties, tokenManager, limiter, restTemplate) { rateWaits += it }

    private val pricesUrl = "https://toss.example.invalid/api/v1/prices?symbols=005930,AAPL"
    private val priceQuery = mapOf("symbols" to "005930,AAPL")
    private val priceRequest = TossApiRequest("/api/v1/prices", TossRateLimitGroup.MARKET_DATA, priceQuery)
    private val pricesType = object : TypeReference<List<PriceSample>>() {}

    private fun expectGet(url: String = pricesUrl): ResponseActions {
        val expected = server.expect(requestTo(url))
        return expected.andExpect(method(HttpMethod.GET))
    }

    private fun ok() = reply(HttpStatus.OK, "success-prices-kr.json")

    private fun call(): List<PriceSample> = client.get(priceRequest, pricesType)

    @Test
    fun `성공하면 Bearer 헤더를 싣고 result 를 data class 로 돌려주며 금액은 BigDecimal 로 정확히 읽는다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet()
            .andExpect(header("Authorization", "Bearer fake-token-a"))
            .andExpect(headerDoesNotExist("X-Tossinvest-Account"))
            .andRespond(reply(HttpStatus.OK, "success-prices-us.json"))

        val prices = call()

        assertEquals(1, prices.size)
        assertEquals("AAPL", prices.single().symbol)
        assertEquals("185.70", prices.single().lastPrice.toPlainString())
        assertEquals("USD", prices.single().currency)
        server.verify()
    }

    @Test
    fun `금액이 JSON 숫자로 와도 자릿수를 잃지 않고 읽는다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        val body = """{"result":[{"symbol":"X","lastPrice":123456789012345.123456,"currency":"KRW"}]}"""
        expectGet().andRespond(replyRaw(HttpStatus.OK, body))

        assertEquals("123456789012345.123456", call().single().lastPrice.toPlainString())
    }

    @Test
    fun `계좌 요청이면 X-Tossinvest-Account 헤더에 accountSeq 를 싣는다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        val body = """{"result":{"symbol":"005930","lastPrice":"72000","currency":"KRW"}}"""
        expectGet("https://toss.example.invalid/api/v1/holdings")
            .andExpect(header("Authorization", "Bearer fake-token-a"))
            .andExpect(header("X-Tossinvest-Account", "1234567890"))
            .andRespond(replyRaw(HttpStatus.OK, body))
        val request = TossApiRequest("/api/v1/holdings", TossRateLimitGroup.ASSET, accountSeq = "1234567890")

        val single = client.get(request, PriceSample::class.java)

        assertEquals("72000", single.lastPrice.toPlainString())
        server.verify()
    }

    @Test
    fun `result 가 빈 배열이면 빈 목록을 돌려준다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet().andRespond(replyRaw(HttpStatus.OK, """{"result":[]}"""))

        assertEquals(emptyList<PriceSample>(), call())
    }

    @Test
    fun `호출마다 요청 그룹의 한도를 먼저 지킨다`() {
        properties.rateLimits = TossRateLimitGroup.entries.associateWith { 1 }
        every { tokenManager.getToken() } returns "fake-token-a"
        repeat(2) { expectGet().andRespond(ok()) }

        call()
        call()

        assertEquals(listOf(1_000L), limiterSlept)
    }

    // ---- 401 ----

    @ParameterizedTest(name = "401 {0} 은 invalidate 뒤 새 토큰으로 1회 재시도한다")
    @CsvSource(
        "invalid-token, error-invalid-token.json",
        "expired-token, error-expired-token.json",
        "token-revoked, error-token-revoked.json",
    )
    fun `401 토큰 오류는 invalidate 하고 새 토큰으로 1회 재시도한다`(code: String, fixture: String) {
        every { tokenManager.getToken() } returnsMany listOf("fake-token-old", "fake-token-new")
        every { tokenManager.invalidate("fake-token-old") } returns true
        expectGet()
            .andExpect(header("Authorization", "Bearer fake-token-old"))
            .andRespond(reply(HttpStatus.UNAUTHORIZED, fixture))
        expectGet().andExpect(header("Authorization", "Bearer fake-token-new")).andRespond(ok())

        assertEquals("005930", call().single().symbol, code)

        verify(exactly = 1) { tokenManager.invalidate("fake-token-old") }
        verify(exactly = 2) { tokenManager.getToken() }
        server.verify()
    }

    @Test
    fun `재시도에서도 401 이면 TOSS_AUTH_FAILED 이고 더 재시도하지 않는다`() {
        every { tokenManager.getToken() } returnsMany listOf("fake-token-old", "fake-token-new")
        every { tokenManager.invalidate(any()) } returns true
        repeat(2) { expectGet().andRespond(reply(HttpStatus.UNAUTHORIZED, "error-token-revoked.json")) }

        val error = assertThrows<TossAuthFailedException> { call() }

        assertEquals("TOSS_AUTH_FAILED", error.errorCode)
        verify(exactly = 1) { tokenManager.invalidate(any()) }
        verify(exactly = 2) { tokenManager.getToken() }
        server.verify()
    }

    @Test
    fun `401 login-user-not-found 는 재발급 없이 TOSS_AUTH_FAILED 이다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet().andRespond(reply(HttpStatus.UNAUTHORIZED, "error-login-user-not-found.json"))

        assertThrows<TossAuthFailedException> { call() }

        verify(exactly = 0) { tokenManager.invalidate(any()) }
        verify(exactly = 1) { tokenManager.getToken() }
        server.verify()
    }

    @ParameterizedTest(name = "401 본문 {1} 은 재시도 없이 TOSS_AUTH_FAILED 이다")
    @CsvSource(
        "'', 빈 본문",
        "'<html>', JSON 아님",
        "'{\"error\":{\"requestId\":\"r\",\"code\":\"some-new-code\",\"message\":\"m\"}}', 모르는 code",
    )
    fun `401 인데 code 를 모르거나 본문을 못 읽으면 재시도 없이 TOSS_AUTH_FAILED 이다`(body: String, reason: String) {
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet().andRespond(replyRaw(HttpStatus.UNAUTHORIZED, body))

        assertThrows<TossAuthFailedException>(reason) { call() }

        verify(exactly = 0) { tokenManager.invalidate(any()) }
        server.verify()
    }

    @Test
    fun `토큰 관리자가 토큰을 못 주면 그 예외가 그대로 나가고 요청은 보내지 않는다`() {
        every { tokenManager.getToken() } throws TossTokenUnavailableException("락 대기 한도 초과")

        assertThrows<TossTokenUnavailableException> { call() }

        server.verify()
    }

    // ---- 403 ----

    @Test
    fun `403 ip-not-allowed 는 재시도 0회이고 TOSS_IP_NOT_ALLOWED 이다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet().andRespond(reply(HttpStatus.FORBIDDEN, "error-ip-not-allowed.json"))

        val error = assertThrows<TossIpNotAllowedException> { call() }

        assertEquals("TOSS_IP_NOT_ALLOWED", error.errorCode)
        verify(exactly = 0) { tokenManager.invalidate(any()) }
        assertTrue(rateWaits.isEmpty())
        server.verify()
    }

    @ParameterizedTest(name = "403 {0} 는 TOSS_AUTH_FAILED 이다")
    @CsvSource("edge-blocked, error-edge-blocked.json", "forbidden, error-forbidden.json")
    fun `403 edge-blocked 와 forbidden 은 재시도 없이 TOSS_AUTH_FAILED 이다`(code: String, fixture: String) {
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet().andRespond(reply(HttpStatus.FORBIDDEN, fixture))

        assertThrows<TossAuthFailedException>(code) { call() }

        verify(exactly = 0) { tokenManager.invalidate(any()) }
        server.verify()
    }

    @Test
    fun `403 본문을 못 읽어도 TOSS_AUTH_FAILED 이다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet().andRespond(replyRaw(HttpStatus.FORBIDDEN, "not json"))

        assertThrows<TossAuthFailedException> { call() }
    }

    // ---- 429 ----

    @Test
    fun `429 Retry-After 만큼 기다린 뒤 재시도해 성공한다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet().andRespond(tooManyRequests("2"))
        expectGet().andExpect(header("Authorization", "Bearer fake-token-a")).andRespond(ok())

        assertEquals("005930", call().single().symbol)

        assertEquals(listOf(2_000L), rateWaits)
        server.verify()
    }

    @ParameterizedTest(name = "Retry-After [{0}] 는 1초로 본다")
    @ValueSource(strings = ["abc", "Wed, 21 Oct 2015 07:28:00 GMT", "-3", " ", "1.5"])
    fun `429 Retry-After 가 숫자가 아니거나 음수이면 1초 기다린다`(header: String) {
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet().andRespond(tooManyRequests(header))
        expectGet().andRespond(ok())

        call()

        assertEquals(listOf(1_000L), rateWaits)
    }

    @Test
    fun `429 Retry-After 헤더가 없으면 1초 기다린다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet().andRespond(tooManyRequests())
        expectGet().andRespond(ok())

        call()

        assertEquals(listOf(1_000L), rateWaits)
    }

    @Test
    fun `429 가 최대 재시도 횟수를 넘어 계속되면 TOSS_RATE_LIMITED 이다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        repeat(3) { expectGet().andRespond(tooManyRequests("1")) }

        val error = assertThrows<TossRateLimitedException> { call() }

        assertEquals("TOSS_RATE_LIMITED", error.errorCode)
        assertEquals(listOf(1_000L, 1_000L), rateWaits, "첫 요청 + 재시도 2회, 대기는 2번")
        server.verify()
    }

    @Test
    fun `Retry-After 가 최대 대기 상한을 넘으면 기다리지 않고 바로 TOSS_RATE_LIMITED 이다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet().andRespond(tooManyRequests("6"))

        assertThrows<TossRateLimitedException> { call() }

        assertTrue(rateWaits.isEmpty(), "상한 5초를 넘는 6초는 잠들지 않음: $rateWaits")
        server.verify()
    }

    @Test
    fun `Retry-After 가 Long 최댓값이어도 곱셈이 넘쳐 상한을 통과하지 않고 바로 TOSS_RATE_LIMITED 이다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet().andRespond(tooManyRequests(Long.MAX_VALUE.toString()))

        assertThrows<TossRateLimitedException> { call() }

        assertTrue(rateWaits.isEmpty(), "$rateWaits")
        server.verify()
    }

    @Test
    fun `재시도마다 기다린 시간을 합쳐 최대 대기 상한을 넘기면 멈춘다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        repeat(2) { expectGet().andRespond(tooManyRequests("3")) }

        assertThrows<TossRateLimitedException> { call() }

        assertEquals(listOf(3_000L), rateWaits, "3초 + 3초는 상한 5초 초과라 두 번째는 잠들지 않음")
        server.verify()
    }

    @Test
    fun `최대 재시도가 0 이면 429 는 기다리지 않고 바로 TOSS_RATE_LIMITED 이다`() {
        properties.rateLimitMaxRetries = 0
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet().andRespond(tooManyRequests("1"))

        assertThrows<TossRateLimitedException> { call() }

        assertTrue(rateWaits.isEmpty())
    }

    @Test
    fun `429 재시도도 한도 슬롯을 다시 쓴다`() {
        properties.rateLimits = TossRateLimitGroup.entries.associateWith { 1 }
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet().andRespond(tooManyRequests("1"))
        expectGet().andRespond(ok())

        call()

        assertEquals(listOf(1_000L), limiterSlept)
    }

    // C-05: 429 를 받으면 그 그룹의 다음 슬롯을 Retry-After 뒤로 밀어, 대기 중에 들어온 다른 호출도 같은 시간을 기다림
    @Test
    fun `429 를 받으면 그 그룹의 다음 슬롯을 Retry-After 뒤로 밀어 다른 호출도 같은 시간을 기다린다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        // 대기하는 동안 다른 스레드가 같은 그룹을 쓰는 상황
        val sharing = TossApiClient(properties, tokenManager, limiter, restTemplate) { millis ->
            rateWaits += millis
            limiter.acquire(TossRateLimitGroup.MARKET_DATA)
        }
        expectGet().andRespond(tooManyRequests("2"))
        expectGet().andRespond(ok())

        sharing.get(priceRequest, pricesType)

        assertEquals(2_000L, limiterSlept.first(), "다른 호출이 밀린 슬롯까지 기다려야 함: $limiterSlept")
    }

    @Test
    fun `재시도하지 않고 포기하는 429 는 그룹 슬롯을 밀지 않는다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet().andRespond(tooManyRequests("6"))
        assertThrows<TossRateLimitedException> { call() }

        limiter.acquire(TossRateLimitGroup.MARKET_DATA)

        assertTrue(limiterSlept.none { it >= 1_000 }, "상한을 넘는 Retry-After 로 그룹을 길게 막지 않음: $limiterSlept")
    }

    // ---- 5xx, 네트워크, 타임아웃 ----

    @Test
    fun `500 maintenance 는 TOSS_MAINTENANCE 이고 재시도하지 않는다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet().andRespond(reply(HttpStatus.INTERNAL_SERVER_ERROR, "error-maintenance.json"))

        val error = assertThrows<TossMaintenanceException> { call() }

        assertEquals("TOSS_MAINTENANCE", error.errorCode)
        server.verify()
    }

    @Test
    fun `503 으로 와도 code 가 maintenance 면 TOSS_MAINTENANCE 이다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet().andRespond(reply(HttpStatus.SERVICE_UNAVAILABLE, "error-maintenance.json"))

        assertThrows<TossMaintenanceException> { call() }
    }

    @ParameterizedTest(name = "{0} 는 TOSS_UNAVAILABLE 이다")
    @ValueSource(ints = [500, 502, 503, 504])
    fun `maintenance 가 아닌 5xx 는 TOSS_UNAVAILABLE 이고 재시도하지 않는다`(status: Int) {
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet().andRespond(reply(HttpStatus.valueOf(status), "error-internal-error.json"))

        val error = assertThrows<TossUnavailableException> { call() }

        assertEquals("TOSS_UNAVAILABLE", error.errorCode)
        server.verify()
    }

    @Test
    fun `5xx 본문이 비었거나 JSON 이 아니어도 TOSS_UNAVAILABLE 이다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        val html = withStatus(HttpStatus.BAD_GATEWAY).contentType(MediaType.TEXT_HTML).body("<html>bad gateway</html>")
        expectGet().andRespond(html)
        expectGet().andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        assertThrows<TossUnavailableException> { call() }
        assertThrows<TossUnavailableException> { call() }
    }

    @Test
    fun `읽기 타임아웃은 TOSS_TIMEOUT 이고 재시도하지 않는다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet().andRespond { throw SocketTimeoutException("Read timed out") }

        val error = assertThrows<TossTimeoutException> { call() }

        assertEquals("TOSS_TIMEOUT", error.errorCode)
        server.verify()
    }

    @Test
    fun `연결 거부와 입출력 오류는 TOSS_UNAVAILABLE 이다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet().andRespond { throw ConnectException("Connection refused") }
        expectGet().andRespond { throw IOException("Connection reset") }

        assertThrows<TossUnavailableException> { call() }
        assertThrows<TossUnavailableException> { call() }
    }

    // ---- 그 밖의 4xx ----

    @ParameterizedTest(name = "{0} 는 TOSS_REQUEST_REJECTED 이다")
    @CsvSource(
        "400, error-invalid-request-symbols.json",
        "404, error-stock-not-found.json",
        "409, error-invalid-request-symbols.json",
        "422, error-invalid-request-symbols.json",
    )
    fun `그 밖의 4xx 는 우리 입력 거부라 A 범주 TOSS_REQUEST_REJECTED 이고 재시도하지 않는다`(status: Int, fixture: String) {
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet().andRespond(reply(HttpStatus.valueOf(status), fixture))

        val error = assertThrows<TossRequestRejectedException> { call() }

        assertEquals("TOSS_REQUEST_REJECTED", error.errorCode)
        assertEquals(HttpStatus.BAD_REQUEST, error.status)
        verify(exactly = 0) { tokenManager.invalidate(any()) }
        server.verify()
    }

    @Test
    fun `4xx 본문이 JSON 이 아니어도 TOSS_REQUEST_REJECTED 이다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet().andRespond(replyRaw(HttpStatus.BAD_REQUEST, "not json"))

        assertThrows<TossRequestRejectedException> { call() }
    }

    // ---- 깨진 성공 응답 (D14) ----

    @ParameterizedTest(name = "200 본문 {1} 은 TOSS_RESPONSE_INVALID 이다")
    @CsvSource(
        "'', 빈 본문",
        "'not json', JSON 아님",
        "'{}', result 누락",
        "'{\"result\":null}', result null",
        "'{\"result\":\"x\"}', result 형태 틀림",
        "'{\"result\":[{\"symbol\":\"A\",\"currency\":\"KRW\"}]}', 필수 lastPrice 누락",
        "'{\"result\":[{\"lastPrice\":\"1\",\"currency\":\"KRW\"}]}', 필수 symbol 누락",
        "'{\"result\":[{\"symbol\":\"A\",\"lastPrice\":null,\"currency\":\"KRW\"}]}', 필수 lastPrice null",
        "'{\"result\":[{\"symbol\":\"A\",\"lastPrice\":\"abc\",\"currency\":\"KRW\"}]}', 숫자 변환 실패",
        "'{\"result\":[{\"symbol\":\"A\",\"lastPrice\":\"\",\"currency\":\"KRW\"}]}', 빈 문자열 숫자",
        "'{\"error\":{\"requestId\":\"r\",\"code\":\"x\",\"message\":\"m\"}}', 200 인데 오류 봉투",
    )
    fun `200 이어도 깨진 본문과 필수 필드 누락 숫자 변환 실패는 TOSS_RESPONSE_INVALID 이고 0 으로 채우지 않는다`(
        body: String,
        reason: String,
    ) {
        every { tokenManager.getToken() } returns "fake-token-a"
        expectGet().andRespond(replyRaw(HttpStatus.OK, body))

        val error = assertThrows<TossResponseInvalidException>(reason) { call() }

        assertEquals("TOSS_RESPONSE_INVALID", error.errorCode)
    }

    @Test
    fun `응답 불일치 예외의 메시지에 응답 값이 실리지 않는다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        val body = """{"result":[{"symbol":"VALUE-LEAK-9","lastPrice":"abc-9","currency":"KRW"}]}"""
        expectGet().andRespond(replyRaw(HttpStatus.OK, body))

        val error = assertThrows<TossResponseInvalidException> { call() }

        assertFalse(error.message.orEmpty().contains("abc-9"), error.message)
        assertFalse(error.message.orEmpty().contains("VALUE-LEAK-9"), error.message)
        assertTrue(error.cause == null || !error.cause!!.message.orEmpty().contains("abc-9"))
    }

    @Test
    fun `모르는 필드와 모르는 enum 문자열 값은 허용한다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        val body = """{"result":[{"symbol":"A","lastPrice":"1","currency":"JPY","extra":{"a":1}}],"meta":1}"""
        expectGet().andRespond(replyRaw(HttpStatus.OK, body))

        assertEquals("JPY", call().single().currency)
    }

    // ---- 비활성, 응답 문구, 로그 ----

    @Test
    fun `토스 설정이 비활성이면 토큰도 요청도 건드리지 않고 TOSS_INTEGRATION_DISABLED 이다`() {
        val disabledClient = TossApiClient(TossProperties(), tokenManager, limiter, restTemplate)

        assertThrows<TossIntegrationDisabledException> { disabledClient.get(priceRequest, pricesType) }

        verify(exactly = 0) { tokenManager.getToken() }
        server.verify()
    }

    @Test
    fun `모든 실패 예외 메시지에 토스 원문이 없다`() {
        every { tokenManager.getToken() } returns "fake-token-a"
        every { tokenManager.invalidate(any()) } returns true
        val cases = listOf(
            HttpStatus.FORBIDDEN to "error-ip-not-allowed.json",
            HttpStatus.FORBIDDEN to "error-forbidden.json",
            HttpStatus.INTERNAL_SERVER_ERROR to "error-maintenance.json",
            HttpStatus.INTERNAL_SERVER_ERROR to "error-internal-error.json",
            HttpStatus.BAD_REQUEST to "error-invalid-request-symbols.json",
            HttpStatus.NOT_FOUND to "error-stock-not-found.json",
            HttpStatus.UNAUTHORIZED to "error-login-user-not-found.json",
        )
        cases.forEach { (status, fixture) -> expectGet().andRespond(reply(status, fixture)) }
        // 토스 message 전문과 requestId, 우리 고정 문구와 겹치는 짧은 조각이 아니라 문장 전체로 비교
        val rawTexts = listOf(
            "요청에 필요한 권한이 부족합니다.",
            "점검 중입니다. 잠시 후 다시 시도해 주세요.",
            "처리 중 문제가 생겼어요. 잠시 후 다시 시도해주세요.",
            "요청이 올바르지 않습니다.",
            "종목을 찾을 수 없습니다.",
            "로그인 정보를 찾을 수 없습니다.",
            "허용되지 않은 IP 입니다.",
            "01HXYZABCDEFG123456789",
        )

        cases.forEach { (_, fixture) ->
            val error = assertThrows<RuntimeException> { call() }
            rawTexts.forEach { raw -> assertFalse(error.message.orEmpty().contains(raw), "$fixture: ${error.message}") }
            assertTrue(error is ServiceUnavailableException || error is TossRequestRejectedException, fixture)
        }
    }

    @Test
    fun `401 재시도와 오류 경로의 로그에 토큰이 없고 accountSeq 는 끝 4자리만 남는다`() {
        every { tokenManager.getToken() } returnsMany listOf("fake-token-OLD-LEAK", "fake-token-NEW-LEAK")
        every { tokenManager.invalidate(any()) } returns true
        val request = TossApiRequest("/api/v1/holdings", TossRateLimitGroup.ASSET, accountSeq = "1234567890")
        val url = "https://toss.example.invalid/api/v1/holdings"
        LogCapture().use { logs ->
            // MockRestServiceServer 는 요청이 나간 뒤 기대를 추가할 수 없어 시나리오마다 초기화
            fun scenario(vararg replies: ResponseCreator) {
                server.reset()
                replies.forEach { expectGet(url).andRespond(it) }
                runCatching { client.get(request, PriceSample::class.java) }
            }
            val revoked = reply(HttpStatus.UNAUTHORIZED, "error-token-revoked.json")
            scenario(revoked, revoked)
            every { tokenManager.getToken() } returns "fake-token-NEW-LEAK"
            scenario(reply(HttpStatus.FORBIDDEN, "error-ip-not-allowed.json"))
            scenario(*Array(3) { tooManyRequests("1") })
            scenario(reply(HttpStatus.INTERNAL_SERVER_ERROR, "error-maintenance.json"))
            scenario(reply(HttpStatus.BAD_REQUEST, "error-invalid-request-symbols.json"))
            scenario(ResponseCreator { throw SocketTimeoutException("Read timed out") })
            scenario(replyRaw(HttpStatus.OK, "{}"))

            // 목 라이브러리 자신의 DEBUG 로그(스텁 응답 값 출력)는 우리 코드 로그가 아니라 제외
            val lines = logs.lines().filterNot { it.startsWith("io.mockk") }
            val forbidden = listOf("fake-token-OLD-LEAK", "fake-token-NEW-LEAK", "1234567890", "Bearer")
            val leaked = lines.filter { line -> forbidden.any { line.contains(it) } }
            assertTrue(leaked.isEmpty(), "비밀값이 로그에 있음: ${leaked.size}건 $leaked")
            assertTrue(lines.any { it.contains("7890") }, "끝 4자리는 남아 식별 가능해야 함")
        }
    }

    @Test
    fun `TossApiRequest 문자열 표현은 accountSeq 를 끝 4자리만 보인다`() {
        val text = TossApiRequest("/api/v1/holdings", TossRateLimitGroup.ASSET, accountSeq = "1234567890").toString()

        assertFalse(text.contains("1234567890"), text)
        assertTrue(text.contains("7890"), text)
    }
}
