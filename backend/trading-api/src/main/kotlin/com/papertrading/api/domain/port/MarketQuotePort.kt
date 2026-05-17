package com.papertrading.api.domain.port

import com.papertrading.api.application.common.result.QuoteSnapshot
import com.papertrading.api.domain.enums.TradingMode

interface MarketQuotePort {
    /** Redis Hash quote:{provider}:{mode}:{ticker} 조회. stale(60초 초과) 또는 키 없으면 null 반환. */
    fun getQuote(tradingMode: TradingMode, ticker: String): QuoteSnapshot?
}
