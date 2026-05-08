package com.papertrading.collector.infra.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.collector.domain.marketfeature.FeatureSnapshot
import com.papertrading.collector.domain.marketfeature.FeatureWindow
import com.papertrading.collector.domain.marketfeature.MinuteBarState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.ListOperations
import org.springframework.data.redis.core.StringRedisTemplate
import java.math.BigDecimal
import java.time.Instant

class MarketFeatureRedisStoreTest {
    private val redisTemplate = mockk<StringRedisTemplate>(relaxed = true)
    private val hashOps = mockk<HashOperations<String, String, String>>(relaxed = true)
    private val listOps = mockk<ListOperations<String, String>>(relaxed = true)
    private val objectMapper = mockk<ObjectMapper>(relaxed = true)
    private val store = MarketFeatureRedisStore(redisTemplate, objectMapper, false)

    @Test
    fun `saveCurrent - key naming and ttl policy 적용`() {
        every { redisTemplate.opsForHash<String, String>() } returns hashOps
        every { redisTemplate.expire(any(), any()) } returns true

        store.saveCurrent("005930", bar("2026-05-08T11:30:00Z"))

        verify { hashOps.putAll("agg:1m:005930:current", any()) }
        verify { redisTemplate.expire("agg:1m:005930:current", RedisKeyPolicy.CURRENT_TTL) }
    }

    @Test
    fun `appendBar - list maxlen and ttl policy 적용`() {
        every { redisTemplate.opsForList() } returns listOps
        every { objectMapper.writeValueAsString(any<MinuteBarState>()) } returns """{"minute":"2026-05-08T11:30:00Z"}"""
        every { listOps.rightPush(any(), any()) } returns 1L
        every { listOps.trim(any(), any(), any()) } just runs
        every { redisTemplate.expire(any(), any()) } returns true

        store.appendBar("005930", bar("2026-05-08T11:30:00Z"))

        verify { listOps.rightPush("bars:1m:005930", any()) }
        verify { listOps.trim("bars:1m:005930", -RedisKeyPolicy.BARS_MAX_LEN, -1) }
        verify { redisTemplate.expire("bars:1m:005930", RedisKeyPolicy.BARS_TTL) }
    }

    @Test
    fun `saveSnapshot - key naming and feature ttl policy 적용`() {
        every { redisTemplate.opsForHash<String, String>() } returns hashOps
        every { redisTemplate.expire(any(), any()) } returns true

        store.saveSnapshot("005930", FeatureWindow.M5, snapshot(FeatureWindow.M5))

        verify { hashOps.putAll("feature:005930:m5", any()) }
        verify { redisTemplate.expire("feature:005930:m5", RedisKeyPolicy.FEATURE_TTL) }
    }

    @Test
    fun `debug flag off - debug ring buffer write 없음`() {
        every { redisTemplate.opsForHash<String, String>() } returns hashOps
        every { redisTemplate.expire(any(), any()) } returns true

        store.saveCurrent("005930", bar("2026-05-08T11:30:00Z"))
        store.saveSnapshot("005930", FeatureWindow.M1, snapshot(FeatureWindow.M1))

        verify(exactly = 0) { listOps.rightPush(match { it.startsWith("debug:tick:") }, any()) }
        verify(exactly = 0) { listOps.trim(match { it.startsWith("debug:tick:") }, any(), any()) }
        verify(exactly = 0) { redisTemplate.expire(match { it.startsWith("debug:tick:") }, any()) }
    }

    @Test
    fun `saveCurrent - persisted field includes minute and price fields`() {
        every { redisTemplate.opsForHash<String, String>() } returns hashOps
        every { redisTemplate.expire(any(), any()) } returns true
        val mapSlot = slot<Map<String, String>>()
        every { hashOps.putAll("agg:1m:005930:current", capture(mapSlot)) } just runs

        store.saveCurrent("005930", bar("2026-05-08T11:30:00Z"))

        val saved = mapSlot.captured
        assert(saved["minute"] == "2026-05-08T11:30:00Z")
        assert(saved["open"] == "100")
        assert(saved["high"] == "101")
        assert(saved["low"] == "99")
        assert(saved["close"] == "100")
    }

    private fun bar(minute: String): MinuteBarState {
        return MinuteBarState(
            minute = minute,
            open = BigDecimal("100"),
            high = BigDecimal("101"),
            low = BigDecimal("99"),
            close = BigDecimal("100"),
            volume = BigDecimal("10"),
            tradeValue = BigDecimal("1000"),
            buyVolume = BigDecimal("6"),
            sellVolume = BigDecimal("4"),
            tickCount = 2,
            startedAt = Instant.parse(minute),
            updatedAt = Instant.parse(minute),
        )
    }

    private fun snapshot(window: FeatureWindow): FeatureSnapshot {
        return FeatureSnapshot(
            window = window,
            open = BigDecimal("100"),
            high = BigDecimal("101"),
            low = BigDecimal("99"),
            close = BigDecimal("100"),
            returnRate = BigDecimal("0.01"),
            volume = BigDecimal("10"),
            tradeValue = BigDecimal("1000"),
            vwap = BigDecimal("100"),
            buyVolume = BigDecimal("6"),
            sellVolume = BigDecimal("4"),
            tradeImbalance = BigDecimal("0.2"),
            tickCount = 3,
            startedAt = Instant.parse("2026-05-08T10:00:00Z"),
            updatedAt = Instant.parse("2026-05-08T10:00:30Z"),
        )
    }
}
