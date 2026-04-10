package com.papertrading.collector.infra.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.collector.domain.kis.KisQuoteEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.StringRedisTemplate
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

class QuoteRedisPublisherTest {

    private val redisTemplate = mockk<StringRedisTemplate>(relaxed = true)
    private val objectMapper = ObjectMapper()
    private val publisher = QuoteRedisPublisher(redisTemplate, objectMapper)

    private val hashOps = mockk<HashOperations<String, String, String>>(relaxed = true)

    private fun event() = KisQuoteEvent(
        ticker = "005930",
        price = BigDecimal("75000"),
        askp1 = BigDecimal("75100"),
        bidp1 = BigDecimal("74900"),
        high = BigDecimal("75500"),
        low = BigDecimal("74500"),
        volume = BigDecimal("1000"),
        receivedAt = Instant.now(),
    )

    @Test
    fun `saveAndPublish — Hash putAll, expire, convertAndSend 호출 확인`() {
        every { redisTemplate.opsForHash<String, String>() } returns hashOps

        publisher.saveAndPublish(event())

        verify { hashOps.putAll("quote:005930", any()) }
        verify { redisTemplate.expire("quote:005930", Duration.ofSeconds(60)) }
        verify { redisTemplate.convertAndSend("quote:005930", any<String>()) }
    }

    @Test
    fun `saveAndPublish — Pub-Sub 메시지에 ticker, price, askp1, bidp1 포함`() {
        every { redisTemplate.opsForHash<String, String>() } returns hashOps
        val messageSlot = slot<String>()
        every { redisTemplate.convertAndSend(any(), capture(messageSlot)) } returns 1L

        publisher.saveAndPublish(event())

        val json = objectMapper.readTree(messageSlot.captured)
        assert(json["ticker"].asText() == "005930")
        assert(json["price"].asText() == "75000")
        assert(json["askp1"].asText() == "75100")
        assert(json["bidp1"].asText() == "74900")
    }
}
