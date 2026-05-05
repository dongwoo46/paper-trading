package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.portfolio.TradingJournal
import org.springframework.data.jpa.repository.JpaRepository

interface TradingJournalRepository : JpaRepository<TradingJournal, Long> {
    fun findByAccountIdOrderByCreatedAtDesc(accountId: Long): List<TradingJournal>
    fun findByAccountIdAndTickerOrderByCreatedAtDesc(accountId: Long, ticker: String): List<TradingJournal>
}
