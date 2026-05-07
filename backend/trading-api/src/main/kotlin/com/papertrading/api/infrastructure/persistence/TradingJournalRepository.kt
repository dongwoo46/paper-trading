package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.portfolio.TradingJournal
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface TradingJournalRepository : JpaRepository<TradingJournal, Long> {
    fun findByAccountIdOrderByCreatedAtDesc(accountId: Long): List<TradingJournal>
    fun findByAccountIdAndTickerOrderByCreatedAtDesc(accountId: Long, ticker: String): List<TradingJournal>
    fun findByAccountId(accountId: Long, pageable: Pageable): Page<TradingJournal>
    fun findByAccountIdAndTicker(accountId: Long, ticker: String, pageable: Pageable): Page<TradingJournal>
}
