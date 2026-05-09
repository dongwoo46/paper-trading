package com.papertrading.collector.application.kis.pipeline

import com.papertrading.collector.application.marketfeature.service.MarketFeatureAggregationService
import com.papertrading.collector.domain.entity.kis.KisOrderbookEvent
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
    private val orderbookParser = mockk<KisOrderbookEventParser>()
    private val publisher = mockk<QuoteRedisPublisher>()
    private val aggregationService = mockk<MarketFeatureAggregationService>()

    private fun buildPipeline() = RawEventPipeline(parser, orderbookParser, publisher, aggregationService)

    private fun sampleQuoteEvent() = KisQuoteEvent(
        ticker = "005930",
        price = BigDecimal("70000"),
        askp1 = BigDecimal("70100"),
        bidp1 = BigDecimal("69900"),
        high = BigDecimal("70200"),
        low = BigDecimal("69800"),
        volume = BigDecimal("100"),
        receivedAt = Instant.parse("2026-05-08T11:30:00Z"),
    )

    private fun sampleOrderbookEvent() = KisOrderbookEvent(
        ticker = "005930",
        askPrices = listOf(BigDecimal("70100"), BigDecimal("70200"), BigDecimal("70300"), BigDecimal("70400"), BigDecimal("70500")),
        bidPrices = listOf(BigDecimal("69900"), BigDecimal("69800"), BigDecimal("69700"), BigDecimal("69600"), BigDecimal("69500")),
        askQtys  = listOf(BigDecimal("100"), BigDecimal("200"), BigDecimal("300"), BigDecimal("400"), BigDecimal("500")),
        bidQtys  = listOf(BigDecimal("150"), BigDecimal("250"), BigDecimal("350"), BigDecimal("450"), BigDecimal("550")),
        receivedAt = Instant.parse("2026-05-08T11:30:00Z"),
    )

    @Test
    fun `H0STCNT0 payload — 기존 경로 유지됨`() {
        val event = sampleQuoteEvent()
        val payload = "0|H0STCNT0|001|payload-data"
        every { parser.parse(payload) } returns event
        every { publisher.saveAndPublish(event) } just runs
        every { aggregationService.onTick(event) } throws IllegalStateException("redis down")

        buildPipeline().publish("kis", payload)

        verify(exactly = 1) { publisher.saveAndPublish(event) }
        verify(exactly = 1) { aggregationService.onTick(event) }
        // orderbookParser must NOT be called for H0STCNT0
        verify(exactly = 0) { orderbookParser.parse(any()) }
    }

    @Test
    fun `H0STASP0 payload — orderbook parser 호출됨`() {
        val obEvent = sampleOrderbookEvent()
        val payload = "0|H0STASP0|001|payload-data"
        every { orderbookParser.parse(payload) } returns obEvent

        buildPipeline().publish("kis", payload)

        verify(exactly = 1) { orderbookParser.parse(payload) }
        // quote publisher must NOT be called for H0STASP0
        verify(exactly = 0) { publisher.saveAndPublish(any()) }
        verify(exactly = 0) { aggregationService.onTick(any()) }
    }

    @Test
    fun `H0STASP0 파싱 실패(null) — 처리 중단`() {
        val payload = "0|H0STASP0|001|bad-data"
        every { orderbookParser.parse(payload) } returns null

        buildPipeline().publish("kis", payload)

        verify(exactly = 1) { orderbookParser.parse(payload) }
        verify(exactly = 0) { publisher.saveAndPublish(any()) }
    }

    @Test
    fun `알 수 없는 TR_ID — 무시됨`() {
        val payload = "0|UNKNOWN_TR|001|payload-data"

        buildPipeline().publish("kis", payload)

        verify(exactly = 0) { parser.parse(any()) }
        verify(exactly = 0) { orderbookParser.parse(any()) }
        verify(exactly = 0) { publisher.saveAndPublish(any()) }
    }

    @Test
    fun `parts 수 부족 — 무시됨`() {
        // payload에 '|' 구분자가 없어 parts.size < 2 조건 충족
        val payload = "onlyOnePart"

        buildPipeline().publish("kis", payload)

        verify(exactly = 0) { parser.parse(any()) }
        verify(exactly = 0) { orderbookParser.parse(any()) }
    }
}