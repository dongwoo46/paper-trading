package com.papertrading.api.application.settlement

import com.papertrading.api.support.withId
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.SettlementStatus
import com.papertrading.api.domain.enums.TransactionType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.account.AccountLedger
import com.papertrading.api.domain.entity.settlement.ReceivableSettlement
import com.papertrading.api.infrastructure.persistence.AccountLedgerRepository
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.ReceivableSettlementRepository
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

    private val ReceivableSettlementRepository = mockk<ReceivableSettlementRepository>()
    private val accountRepository = mockk<AccountRepository>()
    private val accountLedgerRepository = mockk<AccountLedgerRepository>()

    private val processor = SettlementProcessor(
        ReceivableSettlementRepository,
        accountRepository,
        accountLedgerRepository,
    )

    private fun account(deposit: BigDecimal = BigDecimal("1000000")): Account =
        Account.create(
            accountName = "test",
            accountType = AccountType.STOCK,
            tradingMode = TradingMode.LOCAL,
            initialDeposit = deposit,
        ).withId(1L)

    private fun ReceivableSettlement(
        id: Long,
        account: Account,
        amount: BigDecimal = BigDecimal("50000"),
        date: LocalDate = LocalDate.of(2024, 1, 10),
    ): ReceivableSettlement = ReceivableSettlement.create(
        account = account,
        orderId = 100L,
        settlementDate = date,
        amount = amount,
    ).also { it.id = id }

    @Test
    fun `processOne — 계좌 잔액 증가, ps 상태 COMPLETED, AccountLedger(SETTLEMENT) 저장, accountRepository 저장`() {
        val initialDeposit = BigDecimal("1000000")
        val amount = BigDecimal("50000")
        val account = account(initialDeposit)
        val ps = ReceivableSettlement(id = 1L, account = account, amount = amount)

        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        val ledgerSlot = slot<AccountLedger>()
        every { accountLedgerRepository.save(capture(ledgerSlot)) } answers { firstArg() }

        processor.processOne(ps)

        // 계좌 잔액 증가 검증: 1000000 + 50000 = 1050000
        assertEquals(0, BigDecimal("1050000.0000").compareTo(account.availableDeposit))
        assertEquals(0, BigDecimal("1050000.0000").compareTo(account.deposit))

        // ReceivableSettlement.complete() 호출 검증
        assertEquals(SettlementStatus.COMPLETED, ps.status)

        // AccountLedger(SETTLEMENT) 저장 검증
        val ledger = ledgerSlot.captured
        assertEquals(TransactionType.SETTLEMENT, ledger.transactionType)
        assertEquals(0, BigDecimal("50000.0000").compareTo(ledger.amount))
        assertEquals(0, BigDecimal("1050000.0000").compareTo(ledger.balanceAfter))
        assertEquals("settlement-1", ledger.idempotencyKey)

        // dirty checking 사용: 명시적 save 호출 없음
        verify(exactly = 0) { accountRepository.save(any()) }
        verify(exactly = 0) { ReceivableSettlementRepository.save(any()) }
    }

    @Test
    fun `processOne — 계좌 없으면 NoSuchElementException`() {
        val account = account()
        val ps = ReceivableSettlement(id = 5L, account = account)

        every { accountRepository.findByIdWithLock(1L) } returns Optional.empty()

        assertThrows<NoSuchElementException> {
            processor.processOne(ps)
        }

        verify(exactly = 0) { accountRepository.save(any()) }
        verify(exactly = 0) { accountLedgerRepository.save(any()) }
    }

    @Test
    fun `processOne — 이미 COMPLETED 상태면 예외`() {
        val account = account()
        val ps = ReceivableSettlement(id = 7L, account = account)
        ps.complete(ps.settlementDate)
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)

        assertThrows<IllegalStateException> {
            processor.processOne(ps)
        }
    }

    @Test
    fun `processOne — amount 소수점 4자리 스케일 정규화 후 저장`() {
        val account = account(BigDecimal("500000"))
        // 소수점 있는 금액
        val ps = ReceivableSettlement(id = 3L, account = account, amount = BigDecimal("12345.6789"))

        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        val ledgerSlot = slot<AccountLedger>()
        every { accountLedgerRepository.save(capture(ledgerSlot)) } answers { firstArg() }

        processor.processOne(ps)

        assertEquals(BigDecimal("12345.6789"), ledgerSlot.captured.amount)
        assertEquals(SettlementStatus.COMPLETED, ps.status)
    }
}

