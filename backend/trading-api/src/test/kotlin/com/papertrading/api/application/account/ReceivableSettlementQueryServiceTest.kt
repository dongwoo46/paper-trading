package com.papertrading.api.application.account

import com.papertrading.api.application.account.query.ReceivableSettlementFilter
import com.papertrading.api.application.account.result.ReceivableSettlementResult
import com.papertrading.api.domain.enums.SettlementStatus
import com.papertrading.api.infrastructure.persistence.ReceivableSettlementRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class ReceivableSettlementQueryServiceTest {

    private val ReceivableSettlementRepository = mockk<ReceivableSettlementRepository>()
    private lateinit var service: ReceivableSettlementQueryService

    @BeforeEach
    fun setUp() {
        service = ReceivableSettlementQueryService(ReceivableSettlementRepository)
    }

    @Test
    fun `계좌의_정산_예정_목록을_조회한다`() {
        val results = listOf(
            ReceivableSettlementResult(1L, 100L, LocalDate.now().plusDays(2), BigDecimal("500000"), SettlementStatus.PENDING)
        )
        val filter = ReceivableSettlementFilter(status = SettlementStatus.PENDING)
        every { ReceivableSettlementRepository.findByAccountIdAndFilter(1L, filter) } returns results

        val result = service.listReceivableSettlements(1L, filter)

        assertThat(result).hasSize(1)
        assertThat(result[0].status).isEqualTo(SettlementStatus.PENDING)
    }

    @Test
    fun `필터_없이_전체_정산_예정_목록을_조회한다`() {
        val results = listOf(
            ReceivableSettlementResult(1L, 100L, LocalDate.now().plusDays(1), BigDecimal("300000"), SettlementStatus.PENDING),
            ReceivableSettlementResult(2L, 101L, LocalDate.now().plusDays(2), BigDecimal("200000"), SettlementStatus.COMPLETED)
        )
        val filter = ReceivableSettlementFilter()
        every { ReceivableSettlementRepository.findByAccountIdAndFilter(1L, filter) } returns results

        val result = service.listReceivableSettlements(1L, filter)

        assertThat(result).hasSize(2)
    }
}

