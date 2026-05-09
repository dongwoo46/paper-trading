package com.papertrading.collector.application.kis.service

import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

data class OrderbookIngestSnapshot(
    val receivedCount: Long,
    val parseFailCount: Long,
    val parseFailRatePct: Double,
    val lastReceivedAtMs: Long?,
    val lastReceivedAgeMs: Long?,
)

@Component
class OrderbookIngestMetrics {
    private val receivedCount = AtomicLong(0)
    private val parseFailCount = AtomicLong(0)
    private val lastReceivedAtMs = AtomicLong(0)

    fun recordReceived() {
        receivedCount.incrementAndGet()
        lastReceivedAtMs.set(System.currentTimeMillis())
    }

    fun recordParseFail() {
        parseFailCount.incrementAndGet()
    }

    fun snapshot(): OrderbookIngestSnapshot {
        val received = receivedCount.get()
        val failed = parseFailCount.get()
        val lastMs = lastReceivedAtMs.get()

        val parseFailRatePct = if (received == 0L) 0.0 else (failed.toDouble() / received * 100)
        val lastReceivedAtMsOrNull = if (lastMs == 0L) null else lastMs
        val lastReceivedAgeMsOrNull = lastReceivedAtMsOrNull?.let { System.currentTimeMillis() - it }

        return OrderbookIngestSnapshot(
            receivedCount = received,
            parseFailCount = failed,
            parseFailRatePct = parseFailRatePct,
            lastReceivedAtMs = lastReceivedAtMsOrNull,
            lastReceivedAgeMs = lastReceivedAgeMsOrNull,
        )
    }
}