package com.papertrading.api.application.account

import com.papertrading.api.application.account.query.PendingSettlementFilter
import com.papertrading.api.application.account.result.PendingSettlementResult
import com.papertrading.api.domain.enums.SettlementStatus
import com.papertrading.api.infrastructure.persistence.PendingSettlementRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class PendingSettlementQueryServiceTest {

    private val pendingSettlementRepository = mockk<PendingSettlementRepository>()
    private lateinit var service: PendingSettlementQueryService

    @BeforeEach
    fun setUp() {
        service = PendingSettlementQueryService(pendingSettlementRepository)
    }

    @Test
    fun `계좌의_정산_예정_목록을_조회한다`() {
        val results = listOf(
            PendingSettlementResult(1L, 100L, LocalDate.now().plusDays(2), BigDecimal("500000"), SettlementStatus.PENDING)
        )
        val filter = PendingSettlementFilter(status = SettlementStatus.PENDING)
        every { pendingSettlementRepository.findByAccountIdAndFilter(1L, filter) } returns results

        val result = service.listPendingSettlements(1L, filter)

        assertThat(result).hasSize(1)
        assertThat(result[0].status).isEqualTo(SettlementStatus.PENDING)
    }

    @Test
    fun `필터_없이_전체_정산_예정_목록을_조회한다`() {
        val results = listOf(
            PendingSettlementResult(1L, 100L, LocalDate.now().plusDays(1), BigDecimal("300000"), SettlementStatus.PENDING),
            PendingSettlementResult(2L, 101L, LocalDate.now().plusDays(2), BigDecimal("200000"), SettlementStatus.COMPLETED)
        )
        val filter = PendingSettlementFilter()
        every { pendingSettlementRepository.findByAccountIdAndFilter(1L, filter) } returns results

        val result = service.listPendingSettlements(1L, filter)

        assertThat(result).hasSize(2)
    }
}
