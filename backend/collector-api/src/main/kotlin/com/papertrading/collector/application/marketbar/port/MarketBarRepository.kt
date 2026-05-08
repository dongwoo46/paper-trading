package com.papertrading.collector.application.marketbar.port

import com.papertrading.collector.domain.marketbar.MarketBar

interface MarketBarRepository {
    fun findBars(symbol: String, interval: String, limit: Int): List<MarketBar>
}
