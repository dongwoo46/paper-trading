package com.papertrading.api.application.account

import com.papertrading.api.application.account.kis.KisAccountBalancePort
import com.papertrading.api.application.account.kis.KisAccountMode
import com.papertrading.api.application.account.kis.KisBalancePosition
import com.papertrading.api.application.account.kis.KisBalanceSnapshot
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.infrastructure.persistence.PositionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime

class KisAccountQueryServiceTest {

    private val kisAccountBalancePort = mockk<KisAccountBalancePort>()
    private val positionRepository = mockk<PositionRepository>()
    private lateinit var service: KisAccountQueryService
    private val dummyAccount = Account.create(
        accountName = "dummy",
        accountType = AccountType.STOCK,
        tradingMode = TradingMode.LOCAL,
        initialDeposit = BigDecimal.ZERO,
    )

    @BeforeEach
    fun setUp() {
        service = KisAccountQueryService(kisAccountBalancePort, positionRepository)
    }

    @Test
    fun `LIVE 모드면 TTTC8434R TR ID를 사용한다`() {
        every { kisAccountBalancePort.fetchBalance(1L, "TTTC8434R") } returns snapshot()
        every { positionRepository.findByAccountIdAndQuantityGreaterThan(1L, BigDecimal.ZERO) } returns emptyList()

        service.getBalance(1L, KisAccountMode.LIVE)

        verify(exactly = 1) { kisAccountBalancePort.fetchBalance(1L, "TTTC8434R") }
    }

    @Test
    fun `PAPER 모드면 VTTC8434R TR ID를 사용한다`() {
        every { kisAccountBalancePort.fetchBalance(1L, "VTTC8434R") } returns snapshot()
        every { positionRepository.findByAccountIdAndQuantityGreaterThan(1L, BigDecimal.ZERO) } returns emptyList()

        service.getBalance(1L, KisAccountMode.PAPER)

        verify(exactly = 1) { kisAccountBalancePort.fetchBalance(1L, "VTTC8434R") }
    }

    @Test
    fun `BigDecimal 기반으로 요약과 포지션을 매핑한다`() {
        every { kisAccountBalancePort.fetchBalance(any(), any()) } returns snapshot()
        every { positionRepository.findByAccountIdAndQuantityGreaterThan(any(), BigDecimal.ZERO) } returns emptyList()

        val result = service.getBalance(7L, KisAccountMode.PAPER)

        assertThat(result.cashBalance).isEqualByComparingTo("1000000.01")
        assertThat(result.marketValue).isEqualByComparingTo("2000000.02")
        assertThat(result.unrealizedPnl).isEqualByComparingTo("10000.11")
        assertThat(result.returnRate).isEqualByComparingTo("1.23")
        assertThat(result.positions).hasSize(1)
        assertThat(result.positions[0].quantity).isEqualByComparingTo("3.5")
    }

    @Test
    fun `정합성 mismatch를 계산한다`() {
        every { kisAccountBalancePort.fetchBalance(any(), any()) } returns snapshot(
            positions = listOf(
                KisBalancePosition(
                    ticker = "005930",
                    quantity = BigDecimal("3"),
                    avgPrice = BigDecimal("70000"),
                    currentPrice = BigDecimal("71000"),
                    marketValue = BigDecimal("213000"),
                    unrealizedPnl = BigDecimal("3000"),
                    returnRate = BigDecimal("4.28")
                )
            )
        )
        every { positionRepository.findByAccountIdAndQuantityGreaterThan(any(), BigDecimal.ZERO) } returns listOf(
            Position.createWithHolding(
                account = dummyAccount,
                ticker = "005930",
                marketType = MarketType.KOSPI,
                quantity = BigDecimal("1"),
                avgBuyPrice = BigDecimal("70000"),
            ),
            Position.createWithHolding(
                account = dummyAccount,
                ticker = "000660",
                marketType = MarketType.KOSPI,
                quantity = BigDecimal("2"),
                avgBuyPrice = BigDecimal("90000"),
            )
        )

        val result = service.getBalance(1L, KisAccountMode.LIVE)

        assertThat(result.reconciliation.quantityMismatch).containsExactly("005930")
        assertThat(result.reconciliation.missingInKis).containsExactly("000660")
        assertThat(result.reconciliation.missingInLocal).isEmpty()
    }

    private fun snapshot(positions: List<KisBalancePosition> = listOf(
        KisBalancePosition(
            ticker = "005930",
            quantity = BigDecimal("3.5"),
            avgPrice = BigDecimal("70000.00"),
            currentPrice = BigDecimal("71000.00"),
            marketValue = BigDecimal("248500.00"),
            unrealizedPnl = BigDecimal("3500.00"),
            returnRate = BigDecimal("1.43")
        )
    )) = KisBalanceSnapshot(
        asOf = OffsetDateTime.parse("2026-05-02T10:00:00+09:00"),
        cashBalance = BigDecimal("1000000.01"),
        marketValue = BigDecimal("2000000.02"),
        unrealizedPnl = BigDecimal("10000.11"),
        returnRate = BigDecimal("1.23"),
        positions = positions
    )
}
