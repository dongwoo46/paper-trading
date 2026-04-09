package com.papertrading.api.application.account

import com.papertrading.api.application.account.command.CreateAccountCommand
import com.papertrading.api.application.account.command.DepositCommand
import com.papertrading.api.application.account.command.UpdateAccountCommand
import com.papertrading.api.application.account.command.WithdrawCommand
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TransactionType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.model.Account
import com.papertrading.api.domain.model.AccountLedger
import com.papertrading.api.infrastructure.persistence.AccountLedgerRepository
import com.papertrading.api.infrastructure.persistence.AccountRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Optional

class AccountCommandServiceTest {

    private val accountRepository = mockk<AccountRepository>()
    private val accountLedgerRepository = mockk<AccountLedgerRepository>()
    private lateinit var service: AccountCommandService

    @BeforeEach
    fun setUp() {
        service = AccountCommandService(accountRepository, accountLedgerRepository)
    }

    @Test
    fun `계좌를_생성하면_저장된_계좌를_반환한다`() {
        val command = CreateAccountCommand(
            accountName = "테스트계좌",
            accountType = AccountType.STOCK,
            tradingMode = TradingMode.LOCAL,
            initialDeposit = BigDecimal("1000000")
        )
        val accountSlot = slot<Account>()
        every { accountRepository.save(capture(accountSlot)) } answers { accountSlot.captured.apply { } }
        every { accountLedgerRepository.save(any()) } returns mockk()

        val result = service.createAccount(command)

        assertThat(result.accountName).isEqualTo("테스트계좌")
        assertThat(result.deposit).isEqualByComparingTo("1000000")
        verify(exactly = 1) { accountRepository.save(any()) }
    }

    @Test
    fun `입금하면_계좌_잔액이_증가하고_원장이_기록된다`() {
        val account = Account.create("테스트", AccountType.STOCK, TradingMode.LOCAL, BigDecimal("500000"))
        val command = DepositCommand(amount = BigDecimal("100000"), idempotencyKey = "key-001")

        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { accountLedgerRepository.findByIdempotencyKey("key-001") } returns null
        val ledgerSlot = slot<AccountLedger>()
        every { accountLedgerRepository.save(capture(ledgerSlot)) } answers { ledgerSlot.captured }

        service.deposit(1L, command)

        assertThat(account.deposit).isEqualByComparingTo("600000")
        assertThat(ledgerSlot.captured.transactionType).isEqualTo(TransactionType.DEPOSIT)
        assertThat(ledgerSlot.captured.amount).isEqualByComparingTo("100000")
    }

    @Test
    fun `동일한_idempotency_key로_재입금하면_기존_원장을_반환한다`() {
        val existingLedger = mockk<AccountLedger>()
        val account = Account.create("테스트", AccountType.STOCK, TradingMode.LOCAL, BigDecimal("500000"))
        val command = DepositCommand(amount = BigDecimal("100000"), idempotencyKey = "key-001")

        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { accountLedgerRepository.findByIdempotencyKey("key-001") } returns existingLedger

        val result = service.deposit(1L, command)

        assertThat(result).isEqualTo(existingLedger)
        verify(exactly = 0) { accountLedgerRepository.save(any()) }
    }

    @Test
    fun `출금하면_계좌_잔액이_감소하고_원장이_기록된다`() {
        val account = Account.create("테스트", AccountType.STOCK, TradingMode.LOCAL, BigDecimal("500000"))
        val command = WithdrawCommand(amount = BigDecimal("100000"), idempotencyKey = "key-002")

        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { accountLedgerRepository.findByIdempotencyKey("key-002") } returns null
        val ledgerSlot = slot<AccountLedger>()
        every { accountLedgerRepository.save(capture(ledgerSlot)) } answers { ledgerSlot.captured }

        service.withdraw(1L, command)

        assertThat(account.deposit).isEqualByComparingTo("400000")
        assertThat(ledgerSlot.captured.transactionType).isEqualTo(TransactionType.WITHDRAWAL)
    }

    @Test
    fun `동일한_idempotency_key로_재출금하면_기존_원장을_반환한다`() {
        val existingLedger = mockk<AccountLedger>()
        val account = Account.create("테스트", AccountType.STOCK, TradingMode.LOCAL, BigDecimal("500000"))
        val command = WithdrawCommand(amount = BigDecimal("100000"), idempotencyKey = "key-002")

        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { accountLedgerRepository.findByIdempotencyKey("key-002") } returns existingLedger

        val result = service.withdraw(1L, command)

        assertThat(result).isEqualTo(existingLedger)
        verify(exactly = 0) { accountLedgerRepository.save(any()) }
    }

    @Test
    fun `존재하지_않는_계좌에_입금하면_예외를_던진다`() {
        val command = DepositCommand(amount = BigDecimal("100000"), idempotencyKey = "key-003")

        every { accountRepository.findByIdWithLock(99L) } returns Optional.empty()

        assertThatThrownBy { service.deposit(99L, command) }
            .isInstanceOf(NoSuchElementException::class.java)
    }

    @Test
    fun `계좌명을_수정하면_변경된_계좌를_반환한다`() {
        val account = Account.create("기존이름", AccountType.STOCK, TradingMode.LOCAL, BigDecimal.ZERO)
        val command = UpdateAccountCommand(accountName = "새이름")

        every { accountRepository.findById(1L) } returns Optional.of(account)

        val result = service.updateAccount(1L, command)

        assertThat(result.accountName).isEqualTo("새이름")
    }

    @Test
    fun `계좌를_비활성화하면_isActive가_false가_된다`() {
        val account = Account.create("테스트", AccountType.STOCK, TradingMode.LOCAL, BigDecimal.ZERO)
        every { accountRepository.findById(1L) } returns Optional.of(account)

        service.deactivateAccount(1L)

        assertThat(account.isActive).isFalse()
    }
}