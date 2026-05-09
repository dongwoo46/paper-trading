package com.papertrading.collector.presentation.kis

import com.papertrading.collector.application.kis.service.OrderbookIngestMetrics
import com.papertrading.collector.application.kis.service.OrderbookIngestSnapshot
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(OrderbookIngestHealthController::class)
class OrderbookIngestHealthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var orderbookIngestMetrics: OrderbookIngestMetrics

    @Test
    fun `GET 인제스트 헬스 — zero state 200 응답`() {
        given(orderbookIngestMetrics.snapshot()).willReturn(
            OrderbookIngestSnapshot(
                receivedCount = 0L,
                parseFailCount = 0L,
                parseFailRatePct = 0.0,
                lastReceivedAtMs = null,
                lastReceivedAgeMs = null,
            ),
        )

        mockMvc.get("/api/internal/kis/orderbook/ingest-health")
            .andExpect {
                status { isOk() }
                jsonPath("$.receivedCount") { value(0) }
                jsonPath("$.parseFailCount") { value(0) }
                jsonPath("$.parseFailRatePct") { value(0.0) }
                jsonPath("$.lastReceivedAtMs") { value(null as Long?) }
                jsonPath("$.lastReceivedAgeMs") { value(null as Long?) }
            }
    }

    @Test
    fun `GET 인제스트 헬스 — 수신 있을 때 200 응답`() {
        given(orderbookIngestMetrics.snapshot()).willReturn(
            OrderbookIngestSnapshot(
                receivedCount = 100L,
                parseFailCount = 1L,
                parseFailRatePct = 1.0,
                lastReceivedAtMs = 1746791234567L,
                lastReceivedAgeMs = 312L,
            ),
        )

        mockMvc.get("/api/internal/kis/orderbook/ingest-health")
            .andExpect {
                status { isOk() }
                jsonPath("$.receivedCount") { value(100) }
                jsonPath("$.parseFailCount") { value(1) }
                jsonPath("$.parseFailRatePct") { value(1.0) }
                jsonPath("$.lastReceivedAtMs") { value(1746791234567L) }
                jsonPath("$.lastReceivedAgeMs") { value(312) }
            }
    }
}