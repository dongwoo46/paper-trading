package com.papertrading.collector.infra.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.papertrading.collector.domain.marketfeature.MinuteBarState
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import java.math.BigDecimal
import java.time.Instant

class MarketBarRedisRepositoryTest {

    private lateinit var repository: MarketBarRedisRepository
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper().apply {
            registerKotlinModule()
            registerModule(JavaTimeModule())
        }
        val redisTemplate = mockk<StringRedisTemplate>(relaxed = true)
        repository = MarketBarRedisRepository(redisTemplate, objectMapper)
    }

    @Test
    fun `aggregate5m — 5개 1m 바를 하나의 5m MarketBar로 집계`() {
        val baseTime = Instant.parse("2026-05-08T10:00:00Z")
        val bars = (0 until 5).map { i ->
            MinuteBarState(
                minute = baseTime.plusSeconds(i * 60L).toString(),
                open = if (i == 0) BigDecimal("70000") else BigDecimal("70100"),
                high = BigDecimal("70000").add(BigDecimal(i * 100)),
                low = BigDecimal("69500").subtract(BigDecimal(i * 10)),
                close = if (i == 4) BigDecimal("70200") else BigDecimal("70100"),
                volume = BigDecimal("200"),
                tradeValue = BigDecimal("14020000"),
                buyVolume = BigDecimal("100"),
                sellVolume = BigDecimal("100"),
                tickCount = 10,
                startedAt = baseTime.plusSeconds(i * 60L),
                updatedAt = baseTime.plusSeconds(i * 60L + 59),
            )
        }

        val result = repository.aggregate(bars, 5, "005930", "5m")

        assertEquals(1, result.size, "5개의 1m 바는 하나의 5m 버킷으로 집계되어야 한다")
        val bar = result[0]
        assertEquals("005930", bar.symbol)
        assertEquals("5m", bar.interval)
        assertEquals(BigDecimal("70000"), bar.open, "open은 첫 번째 바의 open이어야 한다")
        assertEquals(BigDecimal("70200"), bar.close, "close는 마지막 바의 close이어야 한다")
        assertEquals(BigDecimal("70400"), bar.high, "high는 최대값(70000 + 4*100)이어야 한다")
        assertEquals(BigDecimal("69460"), bar.low, "low는 최소값(69500 - 4*10)이어야 한다")
        assertEquals(BigDecimal("1000"), bar.volume, "volume은 합산(200*5)이어야 한다")
        assertEquals(BigDecimal("70100000"), bar.tradeValue, "tradeValue는 합산(14020000*5)이어야 한다")
        assertEquals(50, bar.tickCount, "tickCount는 합산(10*5)이어야 한다")
        assertTrue(bar.vwap > BigDecimal.ZERO, "vwap은 0보다 커야 한다")
    }

    @Test
    fun `aggregate5m — 빈 리스트 입력 시 빈 결과 반환`() {
        val result = repository.aggregate(emptyList(), 5, "005930", "5m")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `MinuteBarState JSON 직렬화 라운드트립`() {
        val original = MinuteBarState(
            minute = "2026-05-08T10:00:00Z",
            open = BigDecimal("70000"),
            high = BigDecimal("70500"),
            low = BigDecimal("69500"),
            close = BigDecimal("70200"),
            volume = BigDecimal("1000"),
            tradeValue = BigDecimal("70200000"),
            buyVolume = BigDecimal("600"),
            sellVolume = BigDecimal("400"),
            tickCount = 42,
            startedAt = Instant.parse("2026-05-08T10:00:00Z"),
            updatedAt = Instant.parse("2026-05-08T10:00:59Z"),
        )

        val json = objectMapper.writeValueAsString(original)
        val deserialized = objectMapper.readValue(json, MinuteBarState::class.java)

        assertEquals(original.minute, deserialized.minute)
        assertEquals(0, original.open.compareTo(deserialized.open))
        assertEquals(0, original.high.compareTo(deserialized.high))
        assertEquals(0, original.low.compareTo(deserialized.low))
        assertEquals(0, original.close.compareTo(deserialized.close))
        assertEquals(0, original.volume.compareTo(deserialized.volume))
        assertEquals(0, original.tradeValue.compareTo(deserialized.tradeValue))
        assertEquals(original.tickCount, deserialized.tickCount)
        assertEquals(original.startedAt, deserialized.startedAt)
        assertEquals(original.updatedAt, deserialized.updatedAt)
    }
}
