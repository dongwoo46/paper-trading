package com.papertrading.api.application.settlement

import com.papertrading.api.application.settlement.command.ProcessSettlementCommand
import com.papertrading.api.application.settlement.command.ProcessSettlementsCommand
import com.papertrading.api.support.withId
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.SettlementStatus
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.settlement.ReceivableSettlement
import com.papertrading.api.infrastructure.persistence.ReceivableSettlementRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class SettlementCommandServiceTest {

    private val ReceivableSettlementRepository = mockk<ReceivableSettlementRepository>()
    private val settlementProcessor = mockk<SettlementProcessor>()

    private val service = SettlementCommandService(
        ReceivableSettlementRepository,
        settlementProcessor,
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
    fun `PENDING 없을 때 0 반환, processor 호출 없음`() {
        val targetDate = LocalDate.of(2024, 1, 10)

        every {
            ReceivableSettlementRepository.findBySettlementDateLessThanEqualAndStatus(targetDate, SettlementStatus.PENDING)
        } returns emptyList()

        val count = service.processSettlements(ProcessSettlementsCommand(targetDate = targetDate))

        assertEquals(0, count)
        verify(exactly = 0) { settlementProcessor.processOne(any()) }
    }

    @Test
    fun `배치 처리 — 한 건 실패해도 나머지 계속 처리, count=1`() {
        val account1 = account(BigDecimal("100000")).withId(1L)
        val account2 = account(BigDecimal("200000")).withId(2L)

        val ps1 = ReceivableSettlement(id = 10L, account = account1, amount = BigDecimal("10000"))
        val ps2 = ReceivableSettlement(id = 11L, account = account2, amount = BigDecimal("20000"))
        val targetDate = LocalDate.of(2024, 1, 10)

        every {
            ReceivableSettlementRepository.findBySettlementDateLessThanEqualAndStatus(targetDate, SettlementStatus.PENDING)
        } returns listOf(ps1, ps2)

        // ps1 처리 시 예외 발생
        every { settlementProcessor.processOne(ps1) } throws RuntimeException("DB 오류")
        justRun { settlementProcessor.processOne(ps2) }

        val count = service.processSettlements(ProcessSettlementsCommand(targetDate = targetDate))

        // ps1 실패, ps2 성공 → 1건
        assertEquals(1, count)
        verify(exactly = 1) { settlementProcessor.processOne(ps1) }
        verify(exactly = 1) { settlementProcessor.processOne(ps2) }
    }

    @Test
    fun `processSettlement — 정상 케이스 - processor에 위임`() {
        val account = account()
        val ps = ReceivableSettlement(id = 2L, account = account)

        every { ReceivableSettlementRepository.findById(2L) } returns Optional.of(ps)
        justRun { settlementProcessor.processOne(ps) }

        service.processSettlement(ProcessSettlementCommand(ReceivableSettlementId = 2L))

        verify(exactly = 1) { settlementProcessor.processOne(ps) }
    }

    @Test
    fun `processSettlement — ID 없으면 NoSuchElementException`() {
        every { ReceivableSettlementRepository.findById(99L) } returns Optional.empty()

        assertThrows<NoSuchElementException> {
            service.processSettlement(ProcessSettlementCommand(ReceivableSettlementId = 99L))
        }

        verify(exactly = 0) { settlementProcessor.processOne(any()) }
    }

    @Test
    fun `배치 처리 — 모두 성공 시 전체 건수 반환`() {
        val account = account()
        val ps1 = ReceivableSettlement(id = 1L, account = account, amount = BigDecimal("10000"))
        val ps2 = ReceivableSettlement(id = 2L, account = account, amount = BigDecimal("20000"))
        val targetDate = LocalDate.of(2024, 1, 10)

        every {
            ReceivableSettlementRepository.findBySettlementDateLessThanEqualAndStatus(targetDate, SettlementStatus.PENDING)
        } returns listOf(ps1, ps2)
        justRun { settlementProcessor.processOne(any()) }

        val count = service.processSettlements(ProcessSettlementsCommand(targetDate = targetDate))

        assertEquals(2, count)
    }
}

