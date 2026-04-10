package com.papertrading.api.domain.port

interface MarketQuotePort {
    /** Redis Hash quote:{ticker} 조회. stale(60초 초과) 또는 키 없으면 null 반환. */
    fun getQuote(ticker: String): QuoteSnapshot?
}
