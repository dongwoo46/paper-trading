package com.papertrading.api.application.settlement

import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.SettlementStatus
import com.papertrading.api.domain.enums.TransactionType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.model.Account
import com.papertrading.api.domain.model.AccountLedger
import com.papertrading.api.domain.model.PendingSettlement
import com.papertrading.api.infrastructure.persistence.AccountLedgerRepository
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.PendingSettlementRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class SettlementProcessorTest {

    private val pendingSettlementRepository = mockk<PendingSettlementRepository>()
    private val accountRepository = mockk<AccountRepository>()
    private val accountLedgerRepository = mockk<AccountLedgerRepository>()

    private val processor = SettlementProcessor(
        pendingSettlementRepository,
        accountRepository,
        accountLedgerRepository,
    )

    private fun account(deposit: BigDecimal = BigDecimal("1000000")): Account =
        Account.create(
            accountName = "test",
            accountType = AccountType.STOCK,
            tradingMode = TradingMode.LOCAL,
            initialDeposit = deposit,
        ).apply { id = 1L }

    private fun pendingSettlement(
        id: Long,
        account: Account,
        amount: BigDecimal = BigDecimal("50000"),
        date: LocalDate = LocalDate.of(2024, 1, 10),
    ): PendingSettlement = PendingSettlement(
        id = id,
        account = account,
        orderId = 100L,
        settlementDate = date,
        amount = amount,
        status = SettlementStatus.PENDING,
    )

    @Test
    fun `processOne — 계좌 잔액 증가, ps 상태 COMPLETED, AccountLedger(SETTLEMENT) 저장, accountRepository 저장`() {
        val initialDeposit = BigDecimal("1000000")
        val amount = BigDecimal("50000")
        val account = account(initialDeposit)
        val ps = pendingSettlement(id = 1L, account = account, amount = amount)

        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { accountRepository.save(account) } returns account
        every { pendingSettlementRepository.save(ps) } returns ps
        val ledgerSlot = slot<AccountLedger>()
        every { accountLedgerRepository.save(capture(ledgerSlot)) } answers { firstArg() }

        processor.processOne(ps)

        // 계좌 잔액 증가 검증: 1000000 + 50000 = 1050000
        assertEquals(BigDecimal("1050000.0000"), account.availableDeposit)
        assertEquals(BigDecimal("1050000.0000"), account.deposit)

        // pendingSettlement.complete() 호출 검증
        assertEquals(SettlementStatus.COMPLETED, ps.status)

        // AccountLedger(SETTLEMENT) 저장 검증
        val ledger = ledgerSlot.captured
        assertEquals(TransactionType.SETTLEMENT, ledger.transactionType)
        assertEquals(BigDecimal("50000.0000"), ledger.amount)
        assertEquals(BigDecimal("1050000.0000"), ledger.balanceAfter)
        assertEquals("settlement-1", ledger.idempotencyKey)

        // accountRepository.save 호출 검증
        verify(exactly = 1) { accountRepository.save(account) }
    }

    @Test
    fun `processOne — 계좌 없으면 NoSuchElementException`() {
        val account = account()
        val ps = pendingSettlement(id = 5L, account = account)

        every { accountRepository.findByIdWithLock(1L) } returns Optional.empty()

        assertThrows<NoSuchElementException> {
            processor.processOne(ps)
        }

        verify(exactly = 0) { accountRepository.save(any()) }
        verify(exactly = 0) { accountLedgerRepository.save(any()) }
    }

    @Test
    fun `processOne — account id null이면 IllegalArgumentException`() {
        val ps = PendingSettlement(
            id = 7L,
            account = null,
            orderId = 100L,
            settlementDate = LocalDate.of(2024, 1, 10),
            amount = BigDecimal("10000"),
            status = SettlementStatus.PENDING,
        )

        assertThrows<IllegalArgumentException> {
            processor.processOne(ps)
        }

        verify(exactly = 0) { accountRepository.findByIdWithLock(any()) }
    }

    @Test
    fun `processOne — amount 소수점 4자리 스케일 정규화 후 저장`() {
        val account = account(BigDecimal("500000"))
        // 소수점 있는 금액
        val ps = pendingSettlement(id = 3L, account = account, amount = BigDecimal("12345.6789"))

        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { accountRepository.save(account) } returns account
        every { pendingSettlementRepository.save(ps) } returns ps
        val ledgerSlot = slot<AccountLedger>()
        every { accountLedgerRepository.save(capture(ledgerSlot)) } answers { firstArg() }

        processor.processOne(ps)

        assertEquals(BigDecimal("12345.6789"), ledgerSlot.captured.amount)
        assertEquals(SettlementStatus.COMPLETED, ps.status)
    }
}
