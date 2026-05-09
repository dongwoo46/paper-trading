package com.papertrading.collector.application.kis.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OrderbookIngestMetricsTest {

    private fun metrics() = OrderbookIngestMetrics()

    @Test
    fun `recordReceived — receivedCount 증가, lastReceivedAtMs 갱신`() {
        val m = metrics()
        val beforeMs = System.currentTimeMillis()

        m.recordReceived()

        val snap = m.snapshot()
        assertEquals(1L, snap.receivedCount)
        assertNotNull(snap.lastReceivedAtMs)
        val lastMs = snap.lastReceivedAtMs!!
        assert(lastMs >= beforeMs) { "lastReceivedAtMs should be >= beforeMs" }
    }

    @Test
    fun `recordParseFail — parseFailCount 증가`() {
        val m = metrics()
        m.recordReceived()
        m.recordParseFail()

        val snap = m.snapshot()
        assertEquals(1L, snap.parseFailCount)
    }

    @Test
    fun `parseFailRatePct 계산 — receivedCount=0이면 0_0`() {
        val m = metrics()
        val snap = m.snapshot()

        assertEquals(0.0, snap.parseFailRatePct)
        assertNull(snap.lastReceivedAtMs)
        assertNull(snap.lastReceivedAgeMs)
    }

    @Test
    fun `parseFailRatePct 계산 — 정상 비율 검증`() {
        val m = metrics()
        repeat(10) { m.recordReceived() }
        repeat(2) { m.recordParseFail() }

        val snap = m.snapshot()
        assertEquals(10L, snap.receivedCount)
        assertEquals(2L, snap.parseFailCount)
        assertEquals(20.0, snap.parseFailRatePct, 0.001)
        assertNotNull(snap.lastReceivedAtMs)
        assertNotNull(snap.lastReceivedAgeMs)
    }
}
