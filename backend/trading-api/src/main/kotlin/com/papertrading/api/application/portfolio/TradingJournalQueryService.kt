package com.papertrading.api.application.portfolio

import com.papertrading.api.application.portfolio.query.TradingJournalFilter
import com.papertrading.api.application.portfolio.result.TradingJournalResult
import com.papertrading.api.common.exception.AccountNotFoundException
import com.papertrading.api.common.exception.TradingJournalNotFoundException
import com.papertrading.api.common.exception.TradingJournalOwnershipMismatchException
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.TradingJournalRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TradingJournalQueryService(
    private val accountRepository: AccountRepository,
    private val tradingJournalRepository: TradingJournalRepository
) {
    // 특정 계좌의 매매일지 목록 조회 (QueryDSL search)
    fun list(filter: TradingJournalFilter, pageable: Pageable): Page<TradingJournalResult> {
        accountRepository.findById(filter.accountId)
            .orElseThrow { AccountNotFoundException(filter.accountId) }
        val page = tradingJournalRepository.search(filter, pageable)
        return page.map { TradingJournalResult.from(it) }
    }

    fun get(journalId: Long, accountId: Long): TradingJournalResult {
        val journal = tradingJournalRepository.findById(journalId)
            .orElseThrow { TradingJournalNotFoundException(journalId) }
        if (!journal.belongsTo(accountId)) {
            throw TradingJournalOwnershipMismatchException(journalId, accountId)
        }

        return TradingJournalResult.from(journal)
    }
}
