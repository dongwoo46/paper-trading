package com.papertrading.api.presentation.dto.portfolio

import com.papertrading.api.application.portfolio.result.TradingJournalResult
import java.time.Instant

data class TradingJournalResponse(
    val id: Long,
    val accountId: Long,
    val journalType: String,
    val title: String,
    val content: String,
    val orderId: Long?,
    val ticker: String?,
    val sentiment: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?
) {
    companion object {
        fun from(result: TradingJournalResult): TradingJournalResponse = TradingJournalResponse(
            id = result.id,
            accountId = result.accountId,
            journalType = result.journalType,
            title = result.title,
            content = result.content,
            orderId = result.orderId,
            ticker = result.ticker,
            sentiment = result.sentiment,
            createdAt = result.createdAt,
            updatedAt = result.updatedAt
        )
    }
}
