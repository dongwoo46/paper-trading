package com.papertrading.api.application.account

import com.papertrading.api.application.account.query.LedgerFilter
import com.papertrading.api.application.account.result.LedgerResult
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TransactionType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.model.Account
import com.papertrading.api.infrastructure.persistence.AccountLedgerRepository
import com.papertrading.api.infrastructure.persistence.AccountRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional

class AccountQueryServiceTest {

    private val accountRepository = mockk<AccountRepository>()
    private val accountLedgerRepository = mockk<AccountLedgerRepository>()
    private lateinit var service: AccountQueryService

    @BeforeEach
    fun setUp() {
        service = AccountQueryService(accountRepository, accountLedgerRepository)
    }

    @Test
    fun `계좌_id로_조회하면_계좌를_반환한다`() {
        val account = Account.create("테스트", AccountType.STOCK, TradingMode.LOCAL, BigDecimal("100000"))
        every { accountRepository.findById(1L) } returns Optional.of(account)

        val result = service.getAccount(1L)

        assertThat(result.accountName).isEqualTo("테스트")
        assertThat(result.deposit).isEqualByComparingTo("100000")
    }

    @Test
    fun `존재하지_않는_계좌_조회시_예외를_던진다`() {
        every { accountRepository.findById(99L) } returns Optional.empty()

        assertThatThrownBy { service.getAccount(99L) }
            .isInstanceOf(NoSuchElementException::class.java)
    }

    @Test
    fun `전체_계좌_목록을_조회한다`() {
        val accounts = listOf(
            Account.create("계좌1", AccountType.STOCK, TradingMode.LOCAL, BigDecimal.ZERO),
            Account.create("계좌2", AccountType.CRYPTO, TradingMode.LOCAL, BigDecimal.ZERO)
        )
        every { accountRepository.findAll() } returns accounts

        val result = service.listAccounts(tradingMode = null, isActive = null)

        assertThat(result).hasSize(2)
    }

    @Test
    fun `활성_계좌만_필터링하여_조회한다`() {
        val accounts = listOf(
            Account.create("활성계좌", AccountType.STOCK, TradingMode.LOCAL, BigDecimal.ZERO)
        )
        every { accountRepository.findByIsActiveTrue() } returns accounts

        val result = service.listAccounts(tradingMode = null, isActive = true)

        assertThat(result).hasSize(1)
    }

    @Test
    fun `거래모드로_필터링하여_계좌를_조회한다`() {
        val accounts = listOf(
            Account.create("로컬계좌", AccountType.STOCK, TradingMode.LOCAL, BigDecimal.ZERO)
        )
        every { accountRepository.findByTradingModeAndIsActiveTrue(TradingMode.LOCAL) } returns accounts

        val result = service.listAccounts(tradingMode = TradingMode.LOCAL, isActive = true)

        assertThat(result).hasSize(1)
    }

    @Test
    fun `원장을_필터_조건으로_페이징_조회한다`() {
        val filter = LedgerFilter(
            transactionType = TransactionType.DEPOSIT,
            pageable = PageRequest.of(0, 10)
        )
        val ledgerResults = listOf(
            LedgerResult(1L, TransactionType.DEPOSIT, BigDecimal("100000"), BigDecimal("100000"), null, null, null, Instant.now())
        )
        every { accountLedgerRepository.findLedgers(1L, filter) } returns PageImpl(ledgerResults)

        val result = service.getLedgers(1L, filter)

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].transactionType).isEqualTo(TransactionType.DEPOSIT)
    }
}
