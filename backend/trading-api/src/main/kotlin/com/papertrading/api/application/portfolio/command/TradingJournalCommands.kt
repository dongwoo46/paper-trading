package com.papertrading.api.application.portfolio.command

data class CreateTradingJournalCommand(
    val accountId: Long,
    val journalType: String,
    val title: String,
    val content: String,
    val orderId: Long? = null,
    val ticker: String? = null,
    val sentiment: String? = null
)

data class UpdateTradingJournalCommand(
    val accountId: Long,
    val title: String,
    val content: String,
    val sentiment: String? = null
)
