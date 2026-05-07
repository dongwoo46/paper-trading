package com.papertrading.api.application.portfolio.query

data class TradingJournalFilter(
    val accountId: Long,
    val ticker: String? = null
)
