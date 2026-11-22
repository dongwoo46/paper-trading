package com.papertrading.api.infrastructure.toss

import com.papertrading.api.common.exception.ServiceUnavailableException
import com.papertrading.api.common.exception.TossAuthFailedException
import com.papertrading.api.common.exception.TossIntegrationDisabledException
import com.papertrading.api.common.exception.TossIpNotAllowedException
import com.papertrading.api.common.exception.TossRateLimitedException
import com.papertrading.api.common.exception.TossResponseInvalidException
import com.papertrading.api.common.exception.TossTimeoutException
import com.papertrading.api.common.exception.TossUnavailableException
import com.papertrading.api.infrastructure.toss.TossFixtures.reply
import com.papertrading.api.infrastructure.toss.TossFixtures.replyRaw
import com.papertrading.api.support.LogCapture
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
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.http.HttpTimeoutException
import java.util.Base64
import java.util.concurrent.atomic.AtomicLong

// POST /oauth2/token 요청 형식과 OAuth2 오류 해석을 공식 명세 fixture 로 확인 (D13, D15, FLAG-18)
class TossTokenIssuerImplTest {

    // 시험 전용 가짜 값, 실제 토스 키 아님. 특수문자는 form 인코딩 확인용
    private val fakeClientId = "fake-client-id"
    private val fakeSecret = "fake-client-secret&=+LEAKCHECK"

    private val restTemplate = RestTemplate()
    private val server = MockRestServiceServer.bindTo(restTemplate).build()
    private val clock = AtomicLong(0)
    private val slept = mutableListOf<Long>()

    private val properties = TossProperties().apply {
        baseUrl = "https://toss.example.invalid"
        clientId = fakeClientId
        clientSecret = fakeSecret
        tokenEncryptionKey = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })
        rateLimits = mapOf(TossRateLimitGroup.AUTH to 5)
    }
    private val limiter = TossRateLimiter(properties, clock::get) { millis ->
        slept += millis
        clock.addAndGet(millis)
    }
    private val issuer = TossTokenIssuerImpl(properties, restTemplate, limiter)

    private fun expectTokenPost(): ResponseActions {
        val expected = server.expect(requestTo("https://toss.example.invalid/oauth2/token"))
        return expected.andExpect(method(HttpMethod.POST))
    }

    private fun tokenSuccess() = reply(HttpStatus.OK, "token-success.json")

    private fun expectedForm() = LinkedMultiValueMap<String, String>().apply {
        add("grant_type", "client_credentials")
        add("client_id", fakeClientId)
        add("client_secret", fakeSecret)
    }

    @Test
    fun `form 본문에 grant_type client_id client_secret 을 담아 보내고 최상위 응답을 읽는다`() {
        expectTokenPost()
            .andExpect(header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE))
            .andExpect(content().formData(expectedForm()))
            .andRespond(tokenSuccess())

        val token = issuer.issue()

        assertEquals("fake-access-token-fixture", token.accessToken)
        assertEquals(86_400L, token.expiresInSeconds)
        server.verify()
    }

    @Test
    fun `발급 요청 전에 AUTH 그룹 한도를 지킨다`() {
        repeat(2) { expectTokenPost().andRespond(tokenSuccess()) }

        issuer.issue()
        issuer.issue()

        assertEquals(listOf(200L), slept)
    }

    @Test
    fun `토스 설정이 비활성이면 요청을 보내지 않고 TOSS_INTEGRATION_DISABLED 이다`() {
        assertThrows<TossIntegrationDisabledException> {
            TossTokenIssuerImpl(TossProperties(), restTemplate, limiter).issue()
        }

        server.verify()
    }

    @ParameterizedTest(name = "{0} 응답은 TOSS_AUTH_FAILED 이다")
    @CsvSource(
        "400, token-error-invalid-request.json",
        "401, token-error-invalid-client.json",
    )
    fun `400 invalid_request 와 401 invalid_client 는 재시도 없이 TOSS_AUTH_FAILED 이다`(status: Int, fixture: String) {
        expectTokenPost().andRespond(reply(HttpStatus.valueOf(status), fixture))

        val error = assertThrows<TossAuthFailedException> { issuer.issue() }

        assertEquals("TOSS_AUTH_FAILED", error.errorCode)
        server.verify()
    }

    @Test
    fun `403 access_denied 는 허용 IP 가 아니라 TOSS_IP_NOT_ALLOWED 이고 재시도하지 않는다`() {
        expectTokenPost().andRespond(reply(HttpStatus.FORBIDDEN, "token-error-access-denied.json"))

        val error = assertThrows<TossIpNotAllowedException> { issuer.issue() }

        assertEquals("TOSS_IP_NOT_ALLOWED", error.errorCode)
        server.verify()
    }

    @Test
    fun `403 인데 access_denied 가 아니면 TOSS_AUTH_FAILED 이다`() {
        val body = """{"error":"unauthorized_client","error_description":"x"}"""
        expectTokenPost().andRespond(replyRaw(HttpStatus.FORBIDDEN, body))

        assertThrows<TossAuthFailedException> { issuer.issue() }
    }

    @Test
    fun `429 는 토큰 발급 중에는 기다리지 않고 TOSS_RATE_LIMITED 이다`() {
        expectTokenPost().andRespond(TossFixtures.tooManyRequests("1"))

        val error = assertThrows<TossRateLimitedException> { issuer.issue() }

        assertEquals("TOSS_RATE_LIMITED", error.errorCode)
        assertTrue(slept.isEmpty(), "발급 최악 소요를 넘기지 않게 대기하지 않음: $slept")
        server.verify()
    }

    @ParameterizedTest(name = "{0} 응답은 TOSS_UNAVAILABLE 이다")
    @ValueSource(ints = [500, 502, 503])
    fun `5xx 는 TOSS_UNAVAILABLE 이다`(status: Int) {
        expectTokenPost().andRespond(reply(HttpStatus.valueOf(status), "error-internal-error.json"))

        val error = assertThrows<TossUnavailableException> { issuer.issue() }

        assertEquals("TOSS_UNAVAILABLE", error.errorCode)
    }

    @Test
    fun `읽기 타임아웃은 TOSS_TIMEOUT 이다`() {
        expectTokenPost().andRespond { throw SocketTimeoutException("Read timed out") }

        val error = assertThrows<TossTimeoutException> { issuer.issue() }

        assertEquals("TOSS_TIMEOUT", error.errorCode)
    }

    @Test
    fun `JDK 클라이언트의 요청 타임아웃도 TOSS_TIMEOUT 이다`() {
        expectTokenPost().andRespond { throw HttpTimeoutException("request timed out") }

        assertThrows<TossTimeoutException> { issuer.issue() }
    }

    @Test
    fun `연결 거부 같은 네트워크 오류는 TOSS_UNAVAILABLE 이다`() {
        expectTokenPost().andRespond { throw ConnectException("Connection refused") }

        assertThrows<TossUnavailableException> { issuer.issue() }
    }

    @Test
    fun `그 밖의 입출력 오류도 TOSS_UNAVAILABLE 이다`() {
        expectTokenPost().andRespond { throw IOException("Connection reset") }

        assertThrows<TossUnavailableException> { issuer.issue() }
    }

    @ParameterizedTest(name = "본문 {1} 은 TOSS_RESPONSE_INVALID 이다")
    @CsvSource(
        "'', 빈 본문",
        "'not json', JSON 아님",
        "'{}', 필드 전부 누락",
        "'{\"token_type\":\"Bearer\",\"expires_in\":3600}', access_token 누락",
        "'{\"access_token\":\"x\",\"expires_in\":3600}', token_type 누락",
        "'{\"access_token\":\"x\",\"token_type\":\"Bearer\"}', expires_in 누락",
        "'{\"access_token\":\"x\",\"token_type\":\"Bearer\",\"expires_in\":\"abc\"}', expires_in 숫자 아님",
        "'{\"access_token\":\"x\",\"token_type\":\"Bearer\",\"expires_in\":null}', expires_in null(0 으로 채우지 않음)",
        "'{\"access_token\":\"x\",\"token_type\":\"Bearer\",\"expires_in\":\"\"}', expires_in 빈 문자열(0 으로 채우지 않음)",
        "'{\"access_token\":\"x\",\"token_type\":\"Bearer\",\"expires_in\":0}', expires_in 0",
        "'{\"access_token\":\"x\",\"token_type\":\"Bearer\",\"expires_in\":-5}', expires_in 음수",
        "'{\"access_token\":\"\",\"token_type\":\"Bearer\",\"expires_in\":3600}', access_token 빈 문자열",
        "'{\"access_token\":\"x\",\"token_type\":\"Basic\",\"expires_in\":3600}', token_type 가 Bearer 아님",
        "'{\"access_token\":null,\"token_type\":\"Bearer\",\"expires_in\":3600}', access_token null",
        "'null', JSON null",
    )
    fun `200 이어도 깨진 본문이나 필수 필드 누락 이상값은 TOSS_RESPONSE_INVALID 이다`(body: String, reason: String) {
        expectTokenPost().andRespond(replyRaw(HttpStatus.OK, body))

        val error = assertThrows<TossResponseInvalidException> { issuer.issue() }

        assertEquals("TOSS_RESPONSE_INVALID", error.errorCode, reason)
    }

    @Test
    fun `expires_in 을 문자열 숫자로 보내도 읽고 큰 값도 Long 으로 보존한다`() {
        val body = """{"access_token":"fake-access-token-fixture","token_type":"bearer","expires_in":"9223372036854"}"""
        expectTokenPost().andRespond(replyRaw(HttpStatus.OK, body))

        assertEquals(9_223_372_036_854L, issuer.issue().expiresInSeconds)
    }

    @Test
    fun `응답에 모르는 필드가 있어도 읽는다`() {
        val body = """{"access_token":"fake-access-token-fixture","token_type":"Bearer","expires_in":60,"scope":"x"}"""
        expectTokenPost().andRespond(replyRaw(HttpStatus.OK, body))

        assertEquals(60L, issuer.issue().expiresInSeconds)
    }

    @Test
    fun `오류 응답의 본문이 JSON 이 아니어도 상태 코드로 분류한다`() {
        val html = withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.TEXT_HTML).body("<html>denied</html>")
        expectTokenPost().andRespond(html)

        assertThrows<TossAuthFailedException> { issuer.issue() }
    }

    @Test
    fun `토스 오류는 모두 J 범주 예외이고 메시지에 토스 원문이 없다`() {
        val fixtures = listOf(
            HttpStatus.BAD_REQUEST to "token-error-invalid-request.json",
            HttpStatus.UNAUTHORIZED to "token-error-invalid-client.json",
            HttpStatus.FORBIDDEN to "token-error-access-denied.json",
            HttpStatus.INTERNAL_SERVER_ERROR to "error-internal-error.json",
        )
        fixtures.forEach { (status, fixture) -> expectTokenPost().andRespond(reply(status, fixture)) }
        val rawTexts = listOf(
            "Client authentication failed",
            "OAuth 2.0 Parameter",
            "IP address not allowed",
            "처리 중 문제가 생겼어요",
        )

        fixtures.forEach { (_, fixture) ->
            val error = assertThrows<ServiceUnavailableException> { issuer.issue() }
            rawTexts.forEach { raw ->
                assertFalse(error.message.orEmpty().contains(raw), "${error.message} / $fixture")
            }
        }
    }

    // RestTemplate 이 DEBUG 로 요청 본문 객체를 그대로 찍는 경로까지 포함해 로그 전체를 본다
    @Test
    fun `성공과 모든 오류 경로의 로그에 client secret 과 토큰이 없다`() {
        LogCapture().use { logs ->
            expectTokenPost().andRespond(tokenSuccess())
            issuer.issue()
            // MockRestServiceServer 는 요청이 나간 뒤 기대를 추가할 수 없어 시나리오마다 초기화
            listOf(
                HttpStatus.BAD_REQUEST to "token-error-invalid-request.json",
                HttpStatus.UNAUTHORIZED to "token-error-invalid-client.json",
                HttpStatus.FORBIDDEN to "token-error-access-denied.json",
                HttpStatus.INTERNAL_SERVER_ERROR to "error-internal-error.json",
            ).forEach { (status, fixture) ->
                server.reset()
                expectTokenPost().andRespond(reply(status, fixture))
                runCatching { issuer.issue() }
            }
            server.reset()
            expectTokenPost().andRespond { throw SocketTimeoutException("Read timed out") }
            runCatching { issuer.issue() }
            server.reset()
            expectTokenPost().andRespond(replyRaw(HttpStatus.OK, "{}"))
            runCatching { issuer.issue() }

            val forbidden = listOf(fakeSecret, "fake-access-token-fixture", "LEAKCHECK")
            val leaked = logs.lines().filter { line -> forbidden.any { line.contains(it) } }
            assertTrue(leaked.isEmpty(), "비밀값이 로그에 있음: ${leaked.size}건")
        }
    }

    @Test
    fun `TossIssuedToken 문자열 표현에 토큰이 없다`() {
        expectTokenPost().andRespond(tokenSuccess())

        assertFalse(issuer.issue().toString().contains("fake-access-token-fixture"))
    }
}
