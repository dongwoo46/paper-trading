package com.papertrading.api.domain.port

import com.papertrading.api.domain.enums.TradingMode

interface LivePositionCachePort {
    fun save(snapshot: LivePositionSnapshot): LivePositionSnapshot
    fun find(accountId: Long, ticker: String): LivePositionSnapshot?
    fun findByAccountId(accountId: Long): List<LivePositionSnapshot>
    fun findByTicker(ticker: String): List<LivePositionSnapshot>
    fun findByTickerAndMode(ticker: String, tradingMode: TradingMode): List<LivePositionSnapshot>
}
