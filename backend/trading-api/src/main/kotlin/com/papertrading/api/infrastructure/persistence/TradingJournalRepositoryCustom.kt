package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.application.portfolio.query.TradingJournalFilter
import com.papertrading.api.domain.entity.portfolio.TradingJournal
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface TradingJournalRepositoryCustom {
    fun search(filter: TradingJournalFilter, pageable: Pageable): Page<TradingJournal>
}

