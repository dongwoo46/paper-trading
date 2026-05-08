package com.papertrading.collector.presentation.marketbar.dto

data class MarketBarResponse(
    val symbol: String,
    val interval: String,
    val bars: List<BarDto>,
)

data class BarDto(
    val startedAt: String,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: String,
    val tradeValue: String,
    val vwap: String,
    val tickCount: Int,
)
