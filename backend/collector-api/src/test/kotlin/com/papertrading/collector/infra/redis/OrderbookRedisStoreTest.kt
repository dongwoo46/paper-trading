package com.papertrading.collector.infra.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.collector.domain.entity.kis.KisOrderbookEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.StringRedisTemplate
import java.math.BigDecimal
import java.time.Instant

class OrderbookRedisStoreTest {

    private val redisTemplate = mockk<StringRedisTemplate>(relaxed = true)
    private val objectMapper = ObjectMapper()
    private val store = OrderbookRedisStore(redisTemplate, objectMapper)

    private val hashOps = mockk<HashOperations<String, String, String>>(relaxed = true)

    private fun sampleEvent(
        askQtys: List<BigDecimal> = listOf("100", "200", "300", "400", "500").map { BigDecimal(it) },
        bidQtys: List<BigDecimal> = listOf("150", "250", "350", "450", "550").map { BigDecimal(it) },
    ) = KisOrderbookEvent(
        ticker = "005930",
        askPrices = listOf("70100", "70200", "70300", "70400", "70500").map { BigDecimal(it) },
        bidPrices = listOf("69900", "69800", "69700", "69600", "69500").map { BigDecimal(it) },
        askQtys = askQtys,
        bidQtys = bidQtys,
        receivedAt = Instant.parse("2026-05-09T10:00:00Z"),
    )

    @Test
    fun `save — orderbook hash에 bestBid, bestAsk, spread, depth 저장 검증`() {
        every { redisTemplate.opsForHash<String, String>() } returns hashOps
        val mapSlot = slot<Map<String, String>>()
        every { hashOps.putAll(any(), capture(mapSlot)) } returns Unit

        store.save(sampleEvent())

        verify { hashOps.putAll("orderbook:005930", any()) }
        verify { redisTemplate.expire("orderbook:005930", RedisKeyPolicy.ORDERBOOK_TTL) }

        val map = mapSlot.captured
        assertEquals("69900", map["bestBid"])
        assertEquals("70100", map["bestAsk"])
        assertEquals("200", map["spread"])   // 70100 - 69900 = 200
        assertEquals("1500", map["askDepthTopN"])   // 100+200+300+400+500
        assertEquals("1750", map["bidDepthTopN"])   // 150+250+350+450+550
        assertEquals("H0STASP0", map["source"])
        assertEquals("2026-05-09T10:00:00Z", map["timestamp"])
        assertNotNull(map["depthImbalance"])
        assertNotNull(map["bidLevels"])
        assertNotNull(map["askLevels"])
    }

    @Test
    fun `depthImbalance 분모 0일 때 저장 필드 없음`() {
        every { redisTemplate.opsForHash<String, String>() } returns hashOps
        val mapSlot = slot<Map<String, String>>()
        every { hashOps.putAll(any(), capture(mapSlot)) } returns Unit

        val zeroQtys = listOf(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        store.save(sampleEvent(askQtys = zeroQtys, bidQtys = zeroQtys))

        val map = mapSlot.captured
        assertNull(map["depthImbalance"])
    }

    @Test
    fun `load — Hash 데이터 있을 때 OrderbookSnapshot 반환`() {
        every { redisTemplate.opsForHash<String, String>() } returns hashOps
        every { hashOps.entries("orderbook:005930") } returns mapOf(
            "bestBid" to "69900",
            "bestAsk" to "70100",
            "spread" to "200",
            "bidDepthTopN" to "1750",
            "askDepthTopN" to "1500",
            "depthImbalance" to "0.07692308",
            "timestamp" to "2026-05-09T10:00:00Z",
        )

        val snapshot = store.load("005930")

        assertNotNull(snapshot)
        assertEquals(BigDecimal("69900"), snapshot!!.bestBid)
        assertEquals(BigDecimal("70100"), snapshot.bestAsk)
        assertEquals(BigDecimal("200"), snapshot.spread)
        assertEquals(BigDecimal("1750"), snapshot.bidDepthTopN)
        assertEquals(BigDecimal("1500"), snapshot.askDepthTopN)
        assertEquals(BigDecimal("0.07692308"), snapshot.depthImbalance)
        assertEquals(Instant.parse("2026-05-09T10:00:00Z"), snapshot.timestamp)
    }

    @Test
    fun `load — Hash 비어있을 때 null 반환`() {
        every { redisTemplate.opsForHash<String, String>() } returns hashOps
        every { hashOps.entries("orderbook:005930") } returns emptyMap()

        val snapshot = store.load("005930")

        assertNull(snapshot)
    }
}