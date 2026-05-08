package com.papertrading.collector.application.kis.pipeline

import com.papertrading.collector.application.marketfeature.service.MarketFeatureAggregationService
import com.papertrading.collector.domain.entity.kis.KisQuoteEvent
import com.papertrading.collector.infra.redis.QuoteRedisPublisher
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class RawEventPipelineTest {
    private val parser = mockk<KisRawEventParser>()
    private val publisher = mockk<QuoteRedisPublisher>()
    private val aggregationService = mockk<MarketFeatureAggregationService>()

    @Test
    fun `publish - quote publish 유지 및 aggregation 실패 격리`() {
        val event = KisQuoteEvent(
            ticker = "005930",
            price = BigDecimal("70000"),
            askp1 = BigDecimal("70100"),
            bidp1 = BigDecimal("69900"),
            high = BigDecimal("70200"),
            low = BigDecimal("69800"),
            volume = BigDecimal("100"),
            receivedAt = Instant.parse("2026-05-08T11:30:00Z"),
        )
        every { parser.parse(any()) } returns event
        every { publisher.saveAndPublish(event) } just runs
        every { aggregationService.onTick(event) } throws IllegalStateException("redis down")

        val pipeline = RawEventPipeline(parser, publisher, aggregationService)
        pipeline.publish("kis", "payload")

        verify(exactly = 1) { publisher.saveAndPublish(event) }
        verify(exactly = 1) { aggregationService.onTick(event) }
    }
}

