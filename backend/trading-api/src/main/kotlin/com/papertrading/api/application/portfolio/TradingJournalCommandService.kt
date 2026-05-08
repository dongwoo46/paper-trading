package com.papertrading.api.application.portfolio

import com.papertrading.api.application.portfolio.command.CreateTradingJournalCommand
import com.papertrading.api.application.portfolio.command.UpdateTradingJournalCommand
import com.papertrading.api.application.portfolio.result.TradingJournalResult
import com.papertrading.api.common.exception.AccountNotFoundException
import com.papertrading.api.common.exception.TradingJournalNotFoundException
import com.papertrading.api.common.exception.TradingJournalOwnershipMismatchException
import com.papertrading.api.domain.entity.portfolio.TradingJournal
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.TradingJournalRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TradingJournalCommandService(
    private val accountRepository: AccountRepository,
    private val tradingJournalRepository: TradingJournalRepository
) {
    fun create(command: CreateTradingJournalCommand): TradingJournalResult {
        val account = accountRepository.findById(command.accountId)
            .orElseThrow { AccountNotFoundException(command.accountId) }
        val journal = TradingJournal.create(
            account = account,
            journalType = command.journalType,
            title = command.title,
            content = command.content,
            orderId = command.orderId,
            ticker = command.ticker,
            sentiment = command.sentiment
        )
        return TradingJournalResult.from(tradingJournalRepository.save(journal))
    }

    fun update(journalId: Long, command: UpdateTradingJournalCommand): TradingJournalResult {
        val journal = tradingJournalRepository.findById(journalId)
            .orElseThrow { TradingJournalNotFoundException(journalId) }
        if (!journal.belongsTo(command.accountId)) {
            throw TradingJournalOwnershipMismatchException(journalId, command.accountId)
        }
        journal.update(command.title, command.content, command.sentiment)
        return TradingJournalResult.from(journal)
    }
}
