package com.papertrading.api.infrastructure.toss

import com.fasterxml.jackson.core.type.TypeReference
import com.papertrading.api.common.exception.TossAuthFailedException
import com.papertrading.api.infrastructure.redis.RedisDistributedLock
import com.papertrading.api.infrastructure.security.AesGcmTokenCipher
import com.papertrading.api.infrastructure.toss.TossFixtures.reply
import com.papertrading.api.support.SharedRedisContainer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.ResponseActions
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.web.client.RestTemplate
import java.util.Base64

// 실제 토큰 관리자(Redis)와 함께 401 재발급을 확인: 발급 횟수와 다른 프로세스가 먼저 저장한 토큰 재사용 (D12, 토큰 1개 제약)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TossApiClientTokenFlowTest {

    private val keyBytes = ByteArray(32) { (it + 11).toByte() }
    private val cipher = AesGcmTokenCipher(keyBytes)

    private val factory = SharedRedisContainer.newConnectionFactory()
    private val redisTemplate = SharedRedisContainer.newTemplate(factory)
    private val lock = RedisDistributedLock(redisTemplate)

    private val properties = TossProperties().apply {
        baseUrl = "https://toss.example.invalid"
        clientId = "fake-client-id"
        clientSecret = "fake-client-secret"
        tokenEncryptionKey = Base64.getEncoder().encodeToString(keyBytes)
        rateLimits = TossRateLimitGroup.entries.associateWith { 1_000 }
    }

    private val url = "https://toss.example.invalid/api/v1/prices?symbols=005930,AAPL"
    private val query = mapOf("symbols" to "005930,AAPL")
    private val request = TossApiRequest("/api/v1/prices", TossRateLimitGroup.MARKET_DATA, query)
    private val type = object : TypeReference<List<PriceSample>>() {}

    @BeforeEach
    fun clean() {
        redisTemplate.delete(listOf(TossTokenManager.TOKEN_KEY, TossTokenManager.LOCK_KEY))
    }

    @AfterAll
    fun close() {
        factory.destroy()
    }

    private class Fixture(val issuer: FakeTossTokenIssuer, val server: MockRestServiceServer, val client: TossApiClient)

    private fun fixture(): Fixture {
        val restTemplate = RestTemplate()
        val server = MockRestServiceServer.bindTo(restTemplate).build()
        val issuer = FakeTossTokenIssuer(expiresInSeconds = 3_600)
        val manager = TossTokenManager(properties, redisTemplate, lock, issuer)
        val limiter = TossRateLimiter(properties)
        return Fixture(issuer, server, TossApiClient(properties, manager, limiter, restTemplate))
    }

    private fun Fixture.expectGet(): ResponseActions {
        val expected = server.expect(requestTo(url))
        return expected.andExpect(method(HttpMethod.GET))
    }

    // 다른 프로세스가 같은 키로 암호화해 저장한 상황
    private fun store(token: String) {
        redisTemplate.opsForValue().set(TossTokenManager.TOKEN_KEY, cipher.encrypt(token, TossTokenManager.TOKEN_KEY))
    }

    private fun storedToken(): String {
        val raw = requireNotNull(redisTemplate.opsForValue().get(TossTokenManager.TOKEN_KEY))
        return cipher.decrypt(raw, TossTokenManager.TOKEN_KEY)
    }

    @Test
    fun `token-revoked 이면 직전 토큰을 지우고 재발급 1회 뒤 새 토큰으로 재시도해 성공한다`() {
        val f = fixture()
        f.expectGet()
            .andExpect(header("Authorization", "Bearer fake-access-token-1"))
            .andRespond(reply(HttpStatus.UNAUTHORIZED, "error-token-revoked.json"))
        f.expectGet()
            .andExpect(header("Authorization", "Bearer fake-access-token-2"))
            .andRespond(reply(HttpStatus.OK, "success-prices-kr.json"))

        val prices = f.client.get(request, type)

        assertEquals("005930", prices.single().symbol)
        assertEquals(2, f.issuer.callCount, "처음 1회 + 401 뒤 1회")
        f.server.verify()
    }

    @Test
    fun `연속 401 이면 TOSS_AUTH_FAILED 이고 발급은 재시도 때의 1회까지만이다`() {
        val f = fixture()
        repeat(2) { f.expectGet().andRespond(reply(HttpStatus.UNAUTHORIZED, "error-token-revoked.json")) }

        assertThrows<TossAuthFailedException> { f.client.get(request, type) }

        assertEquals(2, f.issuer.callCount, "처음 1회 + 재시도 전 1회, 더 만들지 않음")
        f.server.verify()
    }

    @Test
    fun `401 을 받는 사이 다른 프로세스가 새 토큰을 저장했으면 발급 없이 그 토큰으로 재시도하고 지우지 않는다`() {
        val f = fixture()
        val newer = "다른-프로세스가-저장한-새-토큰"
        f.expectGet().andRespond {
            store(newer)
            reply(HttpStatus.UNAUTHORIZED, "error-token-revoked.json").createResponse(it)
        }
        f.expectGet()
            .andExpect(header("Authorization", "Bearer $newer"))
            .andRespond(reply(HttpStatus.OK, "success-prices-kr.json"))

        f.client.get(request, type)

        assertEquals(1, f.issuer.callCount, "처음 발급 1회뿐, 재시도는 저장돼 있던 토큰을 읽음")
        assertEquals(newer, storedToken())
        f.server.verify()
    }
}
