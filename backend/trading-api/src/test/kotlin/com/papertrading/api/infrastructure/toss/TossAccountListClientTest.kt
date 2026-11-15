package com.papertrading.api.infrastructure.toss

import com.papertrading.api.common.exception.TossIntegrationDisabledException
import com.papertrading.api.common.exception.TossResponseInvalidException
import com.papertrading.api.infrastructure.toss.TossFixtures.reply
import com.papertrading.api.infrastructure.toss.TossFixtures.replyRaw
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
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.web.client.RestTemplate
import java.util.Base64

// 토스 계좌 목록 어댑터: GET /api/v1/accounts, 응답 accountNo 는 끝 4자리만 남김 (D3, 부록 A)
class TossAccountListClientTest {

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
    private val listClient = TossAccountListClient(TossApiClient(properties, tokenManager, limiter, restTemplate))

    private fun expectAccounts() = server.expect(requestTo("https://toss.example.invalid/api/v1/accounts"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer fake-token"))
        .andExpect(headerDoesNotExist("X-Tossinvest-Account"))

    @Test
    fun `계좌 목록을 읽어 accountNo 는 끝 4자리만 accountSeq 는 전체 값 accountType 은 문자열로 돌려준다`() {
        every { tokenManager.getToken() } returns "fake-token"
        expectAccounts().andRespond(reply(HttpStatus.OK, "success-accounts.json"))

        val accounts = listClient.fetchAccounts()

        assertEquals(listOf("8901", "2100"), accounts.map { it.accountNoLast4 })
        assertEquals(listOf(1L, 9_876_543_210L), accounts.map { it.accountSeq })
        assertEquals(listOf("BROKERAGE", "PENSION_SAVINGS"), accounts.map { it.accountType })
        assertFalse(accounts.toString().contains("12345678901"), accounts.toString())
        assertFalse(accounts.toString().contains("98765432100"), accounts.toString())
        server.verify()
    }

    @Test
    fun `계좌 요청은 ACCOUNT 한도 그룹을 한 번 쓰고 계좌 헤더를 싣지 않는다`() {
        every { tokenManager.getToken() } returns "fake-token"
        expectAccounts().andRespond(reply(HttpStatus.OK, "success-accounts.json"))

        listClient.fetchAccounts()

        verify(exactly = 1) { limiter.acquire(TossRateLimitGroup.ACCOUNT) }
        verify(exactly = 1) { limiter.acquire(any()) }
    }

    @Test
    fun `계좌가 없으면 빈 목록이다`() {
        every { tokenManager.getToken() } returns "fake-token"
        expectAccounts().andRespond(reply(HttpStatus.OK, "success-accounts-empty.json"))

        assertTrue(listClient.fetchAccounts().isEmpty())
    }

    @Test
    fun `모르는 accountType 문자열은 그대로 돌려준다`() {
        every { tokenManager.getToken() } returns "fake-token"
        val body = """{"result":[{"accountNo":"12345678901","accountSeq":3,"accountType":"FUTURE_TYPE"}]}"""
        expectAccounts().andRespond(replyRaw(HttpStatus.OK, body))

        assertEquals("FUTURE_TYPE", listClient.fetchAccounts().single().accountType)
    }

    @ParameterizedTest(name = "본문 {0} 은 TOSS_RESPONSE_INVALID")
    @CsvSource(
        delimiter = '|',
        value = [
            "원소 null|{\"result\":[null]}",
            "accountNo 누락|{\"result\":[{\"accountSeq\":1,\"accountType\":\"BROKERAGE\"}]}",
            "accountSeq 누락|{\"result\":[{\"accountNo\":\"1111\",\"accountType\":\"BROKERAGE\"}]}",
            "accountType 누락|{\"result\":[{\"accountNo\":\"1111\",\"accountSeq\":1}]}",
            "accountSeq null|{\"result\":[{\"accountNo\":\"1111\",\"accountSeq\":null,\"accountType\":\"X\"}]}",
            "accountSeq 실수|{\"result\":[{\"accountNo\":\"1111\",\"accountSeq\":1.5,\"accountType\":\"X\"}]}",
            "accountSeq 문자|{\"result\":[{\"accountNo\":\"1111\",\"accountSeq\":\"abc\",\"accountType\":\"X\"}]}",
            "result 누락|{}",
        ],
    )
    fun `필수 필드 누락과 변환 실패와 목록 원소 null 은 TOSS_RESPONSE_INVALID 이다`(label: String, body: String) {
        every { tokenManager.getToken() } returns "fake-token"
        expectAccounts().andRespond(replyRaw(HttpStatus.OK, body))

        assertThrows<TossResponseInvalidException>(label) { listClient.fetchAccounts() }
    }

    @Test
    fun `성공과 응답 불일치 경로의 로그와 예외 문구에 accountNo 전체가 없다`() {
        LogCapture().use { logs ->
            every { tokenManager.getToken() } returns "fake-token"
            expectAccounts().andRespond(reply(HttpStatus.OK, "success-accounts.json"))
            listClient.fetchAccounts()
            server.reset()
            val body = """{"result":[{"accountNo":"12345678901","accountSeq":"abc","accountType":"BROKERAGE"}]}"""
            expectAccounts().andRespond(replyRaw(HttpStatus.OK, body))

            val error = assertThrows<TossResponseInvalidException> { listClient.fetchAccounts() }

            val lines = logs.lines().filterNot { it.startsWith("io.mockk") }
            assertTrue(lines.none { it.contains("12345678901") || it.contains("98765432100") }, "$lines")
            assertFalse(error.message.orEmpty().contains("12345678901"), error.message)
        }
    }

    @Test
    fun `토스 설정이 비활성이면 호출 없이 TOSS_INTEGRATION_DISABLED 이다`() {
        val disabled = TossAccountListClient(TossApiClient(TossProperties(), tokenManager, limiter, restTemplate))

        assertThrows<TossIntegrationDisabledException> { disabled.fetchAccounts() }

        server.verify()
    }
}
