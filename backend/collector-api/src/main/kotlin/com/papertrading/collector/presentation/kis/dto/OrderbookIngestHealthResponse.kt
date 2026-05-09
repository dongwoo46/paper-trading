package com.papertrading.collector.presentation.kis.dto

data class OrderbookIngestHealthResponse(
    val receivedCount: Long,
    val parseFailCount: Long,
    val parseFailRatePct: Double,
    val lastReceivedAtMs: Long?,
    val lastReceivedAgeMs: Long?,
)