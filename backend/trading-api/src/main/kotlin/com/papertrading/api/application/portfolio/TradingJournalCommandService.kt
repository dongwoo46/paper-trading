package com.papertrading.api.application.portfolio

import com.papertrading.api.application.portfolio.command.CreateTradingJournalCommand
import com.papertrading.api.application.portfolio.command.UpdateTradingJournalCommand
import com.papertrading.api.application.portfolio.result.TradingJournalResult
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
            .orElseThrow { NoSuchElementException("계좌를 찾을 수 없습니다. id=${command.accountId}") }
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
            .orElseThrow { NoSuchElementException("거래 일지를 찾을 수 없습니다. id=$journalId") }
        if (!journal.belongsTo(command.accountId)) {
            throw NoSuchElementException("해당 계좌의 거래 일지가 아닙니다. journalId=$journalId, accountId=${command.accountId}")
        }
        journal.update(command.title, command.content, command.sentiment)
        return TradingJournalResult.from(journal)
    }
}
