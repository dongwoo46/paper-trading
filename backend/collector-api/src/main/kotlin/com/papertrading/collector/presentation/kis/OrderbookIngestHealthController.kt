package com.papertrading.collector.presentation.kis

import com.papertrading.collector.application.kis.service.OrderbookIngestMetrics
import com.papertrading.collector.presentation.kis.dto.OrderbookIngestHealthResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/internal/kis/orderbook")
class OrderbookIngestHealthController(
    private val orderbookIngestMetrics: OrderbookIngestMetrics,
) {
    @GetMapping("/ingest-health")
    fun getIngestHealth(): ResponseEntity<OrderbookIngestHealthResponse> {
        val snapshot = orderbookIngestMetrics.snapshot()
        return ResponseEntity.ok(
            OrderbookIngestHealthResponse(
                receivedCount = snapshot.receivedCount,
                parseFailCount = snapshot.parseFailCount,
                parseFailRatePct = snapshot.parseFailRatePct,
                lastReceivedAtMs = snapshot.lastReceivedAtMs,
                lastReceivedAgeMs = snapshot.lastReceivedAgeMs,
            ),
        )
    }
}
