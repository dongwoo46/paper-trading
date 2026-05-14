package com.papertrading.api.application.portfolio

import com.papertrading.api.application.portfolio.command.CreateTradingJournalCommand
import com.papertrading.api.application.portfolio.command.UpdateTradingJournalCommand
import com.papertrading.api.application.portfolio.query.TradingJournalFilter
import com.papertrading.api.common.exception.TradingJournalOwnershipMismatchException
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.portfolio.TradingJournal
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.TradingJournalRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.util.Optional

class TradingJournalCommandQueryServiceTest {
    private val accountRepository = mockk<AccountRepository>()
    private val tradingJournalRepository = mockk<TradingJournalRepository>()
    private lateinit var commandService: TradingJournalCommandService
    private lateinit var queryService: TradingJournalQueryService

    @BeforeEach
    fun setUp() {
        commandService = TradingJournalCommandService(accountRepository, tradingJournalRepository)
        queryService = TradingJournalQueryService(accountRepository, tradingJournalRepository)
    }

    @Test
    fun `create는 journal을 저장하고 결과를 반환한다`() {
        val account = Account.create("acc", AccountType.STOCK, TradingMode.LOCAL, BigDecimal.ZERO)
        setId(account, 1L)
        val command = CreateTradingJournalCommand(
            accountId = 1L,
            journalType = "MANUAL",
            title = "title",
            content = "content",
            ticker = "005930",
            sentiment = "BULLISH"
        )
        val journalSlot = slot<TradingJournal>()

        every { accountRepository.findById(1L) } returns Optional.of(account)
        every { tradingJournalRepository.save(capture(journalSlot)) } answers {
            journalSlot.captured.also { setJournalId(it, 10L) }
        }

        val result = commandService.create(command)

        assertThat(result.id).isEqualTo(10L)
        assertThat(result.accountId).isEqualTo(1L)
        assertThat(result.ticker).isEqualTo("005930")
        verify(exactly = 1) { tradingJournalRepository.save(any()) }
    }

    @Test
    fun `update는 동일 계좌 journal만 수정한다`() {
        val account = Account.create("acc", AccountType.STOCK, TradingMode.LOCAL, BigDecimal.ZERO)
        setId(account, 1L)
        val journal = TradingJournal.create(account = account, journalType = "MANUAL", title = "old", content = "old")
        setJournalId(journal, 9L)
        val command = UpdateTradingJournalCommand(
            accountId = 1L,
            title = "new",
            content = "new",
            sentiment = "NEUTRAL"
        )

        every { tradingJournalRepository.findById(9L) } returns Optional.of(journal)

        val result = commandService.update(9L, command)

        assertThat(result.title).isEqualTo("new")
        assertThat(result.content).isEqualTo("new")
        assertThat(result.sentiment).isEqualTo("NEUTRAL")
    }

    @Test
    fun `update는 accountId와 무관하게 journal을 수정한다`() {
        val account = Account.create("acc", AccountType.STOCK, TradingMode.LOCAL, BigDecimal.ZERO)
        setId(account, 1L)
        val journal = TradingJournal.create(account = account, journalType = "MANUAL", title = "old", content = "old")
        setJournalId(journal, 9L)
        every { tradingJournalRepository.findById(9L) } returns Optional.of(journal)

        val result = commandService.update(9L, UpdateTradingJournalCommand(2L, "new", "new", null))
        assertThat(result.title).isEqualTo("new")
    }

    @Test
    fun `list는 ticker가 있으면 ticker 조건으로 조회한다`() {
        val account = Account.create("acc", AccountType.STOCK, TradingMode.LOCAL, BigDecimal.ZERO)
        setId(account, 1L)
        val journal = TradingJournal.create(
            account = account,
            journalType = "MANUAL",
            title = "t",
            content = "c",
            ticker = "005930"
        )
        setJournalId(journal, 1L)
        every { accountRepository.findById(1L) } returns Optional.of(account)
        every {
            tradingJournalRepository.search(any(), any())
        } returns PageImpl(listOf(journal), PageRequest.of(0, 20), 1)

        val result = queryService.list(TradingJournalFilter(accountId = 1L, ticker = "005930"), PageRequest.of(0, 20))

        assertThat(result.totalElements).isEqualTo(1)
    }

    @Test
    fun `get은 account mismatch면 예외를 던진다`() {
        val account = Account.create("acc", AccountType.STOCK, TradingMode.LOCAL, BigDecimal.ZERO)
        setId(account, 1L)
        val journal = TradingJournal.create(account = account, journalType = "MANUAL", title = "old", content = "old")
        setJournalId(journal, 9L)
        every { tradingJournalRepository.findById(9L) } returns Optional.of(journal)

        assertThatThrownBy { queryService.get(9L, 2L) }
            .isInstanceOf(TradingJournalOwnershipMismatchException::class.java)
    }

    private fun setId(account: Account, id: Long) {
        val field = Account::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(account, id)
    }

    private fun setJournalId(journal: TradingJournal, id: Long) {
        val field = TradingJournal::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(journal, id)
    }
}
