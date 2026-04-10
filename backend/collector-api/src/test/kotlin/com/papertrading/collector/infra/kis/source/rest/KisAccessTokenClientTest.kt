package com.papertrading.collector.infra.kis.source.rest

import com.papertrading.collector.infra.kis.KisProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class KisAccessTokenClientTest {

    private val properties = mockk<KisProperties>()
    private val rateLimiter = mockk<KisRateLimiter>()
    private val tokenRedisStore = mockk<KisTokenRedisStore>()
    private val webClientBuilder = mockk<WebClient.Builder>(relaxed = true)

    private val client = KisAccessTokenClient(properties, rateLimiter, tokenRedisStore, webClientBuilder)

    @Test
    fun `Redis에 유효한 토큰이 있으면 캐시 히트 — KIS API 호출 없음`() {
        every { tokenRedisStore.findValidToken("paper") } returns "cached-token"

        val result = client.issueAccessToken("paper").block()

        assertEquals("cached-token", result)
        verify(exactly = 0) { rateLimiter.acquireApproval(any()) }
    }

    @Test
    fun `mode 대문자 입력 시 lowercase로 정규화하여 Redis 조회`() {
        every { tokenRedisStore.findValidToken("paper") } returns "cached-token"

        val result = client.issueAccessToken("PAPER").block()

        assertEquals("cached-token", result)
        verify { tokenRedisStore.findValidToken("paper") }
    }

    @Test
    fun `Redis 토큰 없으면 캐시 미스 — rateLimiter acquireApproval 호출`() {
        every { tokenRedisStore.findValidToken("paper") } returns null
        every { properties.appKeyFor("paper") } returns "key"
        every { properties.appSecretFor("paper") } returns "secret"
        every { properties.tokenUrlFor("paper") } returns "https://example.com/token"
        every { rateLimiter.acquireApproval("paper") } returns mockk(relaxed = true)

        runCatching { client.issueAccessToken("paper").block() }

        verify { rateLimiter.acquireApproval("paper") }
    }
}