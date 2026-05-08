package com.papertrading.collector.presentation.marketfeature.dto

data class MarketFeatureResponse(
    val symbol: String,
    val asOf: String,
    val features: List<FeatureItemResponse>,
)

data class FeatureItemResponse(
    val window: String,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val returnRate: String,
    val volume: String,
    val tradeValue: String,
    val vwap: String,
    val buyVolume: String,
    val sellVolume: String,
    val tradeImbalance: String,
    val tickCount: Int,
    val startedAt: String,
    val updatedAt: String,
)

