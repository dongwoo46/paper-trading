package com.papertrading.api.application.settlement

import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.SettlementStatus
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.model.Account
import com.papertrading.api.domain.model.PendingSettlement
import com.papertrading.api.infrastructure.persistence.PendingSettlementRepository
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

    private val pendingSettlementRepository = mockk<PendingSettlementRepository>()
    private val settlementProcessor = mockk<SettlementProcessor>()

    private val service = SettlementCommandService(
        pendingSettlementRepository,
        settlementProcessor,
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
    fun `PENDING 없을 때 0 반환, processor 호출 없음`() {
        val targetDate = LocalDate.of(2024, 1, 10)

        every {
            pendingSettlementRepository.findBySettlementDateLessThanEqualAndStatus(targetDate, SettlementStatus.PENDING)
        } returns emptyList()

        val count = service.processSettlements(targetDate)

        assertEquals(0, count)
        verify(exactly = 0) { settlementProcessor.processOne(any()) }
    }

    @Test
    fun `배치 처리 — 한 건 실패해도 나머지 계속 처리, count=1`() {
        val account1 = account(BigDecimal("100000")).also { it.id = 1L }
        val account2 = account(BigDecimal("200000")).also { it.id = 2L }

        val ps1 = pendingSettlement(id = 10L, account = account1, amount = BigDecimal("10000"))
        val ps2 = pendingSettlement(id = 11L, account = account2, amount = BigDecimal("20000"))
        val targetDate = LocalDate.of(2024, 1, 10)

        every {
            pendingSettlementRepository.findBySettlementDateLessThanEqualAndStatus(targetDate, SettlementStatus.PENDING)
        } returns listOf(ps1, ps2)

        // ps1 처리 시 예외 발생
        every { settlementProcessor.processOne(ps1) } throws RuntimeException("DB 오류")
        justRun { settlementProcessor.processOne(ps2) }

        val count = service.processSettlements(targetDate)

        // ps1 실패, ps2 성공 → 1건
        assertEquals(1, count)
        verify(exactly = 1) { settlementProcessor.processOne(ps1) }
        verify(exactly = 1) { settlementProcessor.processOne(ps2) }
    }

    @Test
    fun `processSettlement — 정상 케이스 - processor에 위임`() {
        val account = account()
        val ps = pendingSettlement(id = 2L, account = account)

        every { pendingSettlementRepository.findById(2L) } returns Optional.of(ps)
        justRun { settlementProcessor.processOne(ps) }

        service.processSettlement(2L)

        verify(exactly = 1) { settlementProcessor.processOne(ps) }
    }

    @Test
    fun `processSettlement — ID 없으면 NoSuchElementException`() {
        every { pendingSettlementRepository.findById(99L) } returns Optional.empty()

        assertThrows<NoSuchElementException> {
            service.processSettlement(99L)
        }

        verify(exactly = 0) { settlementProcessor.processOne(any()) }
    }

    @Test
    fun `배치 처리 — 모두 성공 시 전체 건수 반환`() {
        val account = account()
        val ps1 = pendingSettlement(id = 1L, account = account, amount = BigDecimal("10000"))
        val ps2 = pendingSettlement(id = 2L, account = account, amount = BigDecimal("20000"))
        val targetDate = LocalDate.of(2024, 1, 10)

        every {
            pendingSettlementRepository.findBySettlementDateLessThanEqualAndStatus(targetDate, SettlementStatus.PENDING)
        } returns listOf(ps1, ps2)
        justRun { settlementProcessor.processOne(any()) }

        val count = service.processSettlements(targetDate)

        assertEquals(2, count)
    }
}
