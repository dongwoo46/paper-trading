package com.papertrading.collector.application.subscriptions.service

import com.papertrading.collector.application.kis.service.KisRestWatchlistService
import com.papertrading.collector.application.kis.service.KisWsSubscriptionService
import com.papertrading.collector.infra.kis.KisProperties
import com.papertrading.collector.infra.redis.RedisSetClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SubscriptionRoutingServiceTest {

    private lateinit var redisSetClient: RedisSetClient
    private lateinit var wsSubscriptionService: KisWsSubscriptionService
    private lateinit var restWatchlistService: KisRestWatchlistService
    private lateinit var kisProperties: KisProperties
    private lateinit var service: SubscriptionRoutingService

    @BeforeEach
    fun setUp() {
        redisSetClient = mockk(relaxed = true)
        wsSubscriptionService = mockk()
        restWatchlistService = mockk()
        kisProperties = mockk()
        every { kisProperties.maxRealtimeRegistrations } returns 40
        service = SubscriptionRoutingService(redisSetClient, wsSubscriptionService, restWatchlistService, kisProperties)
    }

    @Test
    fun `addFavorite returns already_exists for duplicate symbol`() {
        every { redisSetClient.members("subscriptions:favorites:paper:ws") } returns listOf("005930")
        every { redisSetClient.size("subscriptions:favorites:paper:ws") } returns 1L

        val response = service.addFavorite(mode = " paper ", channel = " WS ", symbol = "005930")

        assertEquals("already_exists", response.status)
        assertEquals("paper", response.mode)
        assertEquals("ws", response.channel)
        assertEquals("005930", response.symbol)
        assertEquals(1, response.totalSelected)
    }

    @Test
    fun `removeFavorite returns not_found when symbol does not exist`() {
        every { redisSetClient.members("subscriptions:favorites:paper:ws") } returns emptyList()
        every { redisSetClient.size("subscriptions:favorites:paper:ws") } returns 0L

        val response = service.removeFavorite(mode = "paper", channel = "ws", symbol = "005930")

        assertEquals("not_found", response.status)
        assertEquals(0, response.totalSelected)
    }

    @Test
    fun `addFavorite returns invalid_mode for unsupported mode`() {
        val response = service.addFavorite(mode = "demo", channel = "ws", symbol = "005930")

        assertEquals("invalid_mode", response.status)
    }

    @Test
    fun `addFavorite returns invalid_channel for unsupported channel`() {
        val response = service.addFavorite(mode = "paper", channel = "stream", symbol = "005930")

        assertEquals("invalid_channel", response.status)
    }

    @Test
    fun `addStrategySymbol returns invalid_symbol for malformed symbol`() {
        val response = service.addStrategySymbol(mode = "paper", symbol = "bad symbol")

        assertEquals("invalid_symbol", response.status)
    }

    @Test
    fun `addStrategySymbol normalizes to uppercase and returns added`() {
        every { redisSetClient.members("subscriptions:strategy-symbols:paper") } returns emptyList()
        every { redisSetClient.size("subscriptions:strategy-symbols:paper") } returns 1L

        val response = service.addStrategySymbol(mode = "paper", symbol = " aapl ")

        assertEquals("added", response.status)
        assertEquals("AAPL", response.symbol)
        verify(exactly = 1) { redisSetClient.add("subscriptions:strategy-symbols:paper", "AAPL") }
    }

    @Test
    fun `getRoutingStatus includes sources and ws rest outputs`() {
        every { wsSubscriptionService.listSymbols("paper") } returns listOf("005930", "000660")
        every { restWatchlistService.listSymbols("paper") } returns listOf("035420")
        every { redisSetClient.members("kis:ws:paper") } returns listOf("005930")
        every { redisSetClient.members("kis:rest:paper") } returns emptyList()
        every { redisSetClient.members("subscriptions:favorites:paper:ws") } returns listOf("000660")
        every { redisSetClient.members("subscriptions:favorites:paper:rest") } returns listOf("251340")
        every { redisSetClient.members("subscriptions:strategy-symbols:paper") } returns listOf("035420")

        val response = service.getRoutingStatus("paper")

        assertEquals("ok", response.status)
        assertEquals("paper", response.mode)
        assertEquals(listOf("000660", "005930"), response.ws.symbols)
        assertEquals(listOf("035420"), response.rest.symbols)
        assertEquals(listOf("005930"), response.sources.manual)
        assertEquals(listOf("000660", "251340"), response.sources.favorites)
        assertEquals(listOf("035420"), response.sources.strategyPriority)
    }
}
