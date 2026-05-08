package com.papertrading.api.application.portfolio.result

import com.papertrading.api.common.exception.EntityMappingException
import com.papertrading.api.domain.entity.portfolio.TradingJournal
import java.time.Instant

data class TradingJournalResult(
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
        fun from(journal: TradingJournal): TradingJournalResult = TradingJournalResult(
            id = journal.id ?: throw EntityMappingException("journal id가 없습니다."),
            accountId = journal.account?.id ?: throw EntityMappingException("account id가 없습니다."),
            journalType = journal.journalType ?: "",
            title = journal.title ?: "",
            content = journal.content ?: "",
            orderId = journal.orderId,
            ticker = journal.ticker,
            sentiment = journal.sentiment,
            createdAt = journal.createdAt,
            updatedAt = journal.updatedAt
        )
    }
}
