package com.papertrading.api.infrastructure.redis

import com.papertrading.api.domain.enums.TradingMode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Instant

class RedisMarketQuoteAdapterTest {

    private val hashOperations = mockk<HashOperations<String, String, String>>()
    private val redisTemplate = mockk<StringRedisTemplate>().also {
        every { it.opsForHash<String, String>() } returns hashOperations
    }
    private val adapter = RedisMarketQuoteAdapter(redisTemplate)

    // 새 TradingMode 를 추가하면 이 표에 시세 키를 적어야 통과한다
    private val expectedKeyPrefix = mapOf(
        TradingMode.LOCAL to "quote:local:",
        TradingMode.KIS_PAPER to "quote:kis:paper:",
        TradingMode.KIS_LIVE to "quote:kis:live:",
        TradingMode.UPBIT_LIVE to "quote:upbit:live:",
        TradingMode.TOSS_LIVE to "quote:toss:live:",
    )

    @Test
    fun `모드 누락 표 — 모든 TradingMode 가 자기 시세 키를 조회한다`() {
        assertEquals(TradingMode.entries.toSet(), expectedKeyPrefix.keys)

        for ((mode, prefix) in expectedKeyPrefix) {
            every { hashOperations.entries("${prefix}005930") } returns emptyMap()

            assertNull(adapter.getQuote(mode, "005930"), "$mode")
            verify(exactly = 1) { hashOperations.entries("${prefix}005930") }
        }
    }

    @Test
    fun `TOSS_LIVE 시세 키는 종목코드를 대문자로 바꿔 조회한다`() {
        every { hashOperations.entries("quote:toss:live:AAPL") } returns mapOf(
            "price" to "190.1234",
            "askp1" to "190.2000",
            "bidp1" to "190.1000",
            "updatedAt" to Instant.now().toEpochMilli().toString(),
        )

        val quote = adapter.getQuote(TradingMode.TOSS_LIVE, "aapl")

        assertEquals(TradingMode.TOSS_LIVE, quote?.tradingMode)
        assertEquals(0, java.math.BigDecimal("190.1234").compareTo(quote?.price))
    }
}
