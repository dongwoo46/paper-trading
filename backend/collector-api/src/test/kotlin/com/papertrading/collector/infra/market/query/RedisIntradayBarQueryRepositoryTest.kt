package com.papertrading.collector.infra.market.query

import com.papertrading.collector.application.market.service.MarketIndicatorsQuery
import com.papertrading.collector.domain.market.indicator.Interval
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ZSetOperations
import java.math.BigDecimal
import java.time.Instant

class RedisIntradayBarQueryRepositoryTest {
	@Test
	fun `from-to 요청 시 score range 조회와 시간 필터를 적용`() {
		val redisTemplate = mockk<StringRedisTemplate>()
		val zset = mockk<ZSetOperations<String, String>>()
		every { redisTemplate.opsForZSet() } returns zset
		every {
			zset.rangeByScore(
				"bars:1m:005930",
				Instant.parse("2026-05-01T00:00:00Z").toEpochMilli().toDouble(),
				Instant.parse("2026-05-01T00:02:00Z").toEpochMilli().toDouble(),
			)
		} returns linkedSetOf(
			"2026-04-30T23:59:00Z,99",
			"2026-05-01T00:00:00Z,100",
			"2026-05-01T00:01:00Z,101",
		)

		val repository = RedisIntradayBarQueryRepository(redisTemplate)
		val result = repository.load(
			symbol = "005930",
			interval = Interval.ONE_MINUTE,
			request = baseRequest().copy(from = Instant.parse("2026-05-01T00:00:00Z"), to = Instant.parse("2026-05-01T00:02:00Z"), limit = null),
		)

		assertEquals(2, result.size)
		assertEquals(BigDecimal("100"), result[0].close)
		assertEquals(BigDecimal("101"), result[1].close)
		verify(exactly = 0) { zset.reverseRange(any(), any(), any()) }
	}

	private fun baseRequest() = MarketIndicatorsQuery(
		symbol = "005930",
		interval = "1m",
		limit = 30,
		from = null,
		to = null,
		indicators = "bb",
		bbPeriod = null,
		bbStdDev = null,
		rsiPeriod = null,
		macdFast = null,
		macdSlow = null,
		macdSignal = null,
	)
}
