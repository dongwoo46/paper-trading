package com.papertrading.api.domain.entity.portfolio

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TradingMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TradingJournalTest {

    @Test
    fun create_필드가_정상_설정된다() {
        val account = createAccount()

        val journal = TradingJournal.create(
            account = account,
            journalType = "TRADE",
            title = "삼성전자 매수",
            content = "실적 개선 기대로 매수",
            orderId = 1L,
            ticker = "005930",
            sentiment = "BULLISH"
        )

        assertEquals(account, journal.account)
        assertEquals("TRADE", journal.journalType)
        assertEquals("삼성전자 매수", journal.title)
        assertEquals("실적 개선 기대로 매수", journal.content)
        assertEquals(1L, journal.orderId)
        assertEquals("005930", journal.ticker)
        assertEquals("BULLISH", journal.sentiment)
    }

    @Test
    fun create_선택_필드_없이_생성된다() {
        val account = createAccount()

        val journal = TradingJournal.create(
            account = account,
            journalType = "MEMO",
            title = "시장 메모",
            content = "오늘 시장 분석"
        )

        assertNull(journal.orderId)
        assertNull(journal.ticker)
        assertNull(journal.sentiment)
    }

    @Test
    fun updateContent_제목과_내용을_변경한다() {
        val journal = createJournal()

        journal.updateContent("새 제목", "새 내용")

        assertEquals("새 제목", journal.title)
        assertEquals("새 내용", journal.content)
    }

    @Test
    fun updateContent_제목이_공백이면_예외를_던진다() {
        val journal = createJournal()

        assertThrows(IllegalArgumentException::class.java) {
            journal.updateContent("  ", "내용")
        }
    }

    @Test
    fun updateContent_내용이_공백이면_예외를_던진다() {
        val journal = createJournal()

        assertThrows(IllegalArgumentException::class.java) {
            journal.updateContent("제목", "  ")
        }
    }

    @Test
    fun updateContent_실패해도_기존_값이_유지된다() {
        val journal = createJournal(title = "원래 제목", content = "원래 내용")

        assertThrows(IllegalArgumentException::class.java) {
            journal.updateContent("  ", "새 내용")
        }

        assertEquals("원래 제목", journal.title)
        assertEquals("원래 내용", journal.content)
    }

    @Test
    fun updateSentiment_심리_태그를_변경한다() {
        val journal = createJournal(sentiment = "BULLISH")

        journal.updateSentiment("BEARISH")

        assertEquals("BEARISH", journal.sentiment)
    }

    @Test
    fun updateSentiment_null로_초기화한다() {
        val journal = createJournal(sentiment = "BULLISH")

        journal.updateSentiment(null)

        assertNull(journal.sentiment)
    }

    private fun createAccount(): Account = Account.create(
        accountName = "test-account",
        accountType = AccountType.STOCK,
        tradingMode = TradingMode.LOCAL,
        initialDeposit = BigDecimal("100000.0000")
    )

    private fun createJournal(
        title: String = "테스트 제목",
        content: String = "테스트 내용",
        sentiment: String? = null
    ): TradingJournal = TradingJournal.create(
        account = createAccount(),
        journalType = "MEMO",
        title = title,
        content = content,
        sentiment = sentiment
    )
}