package com.papertrading.api.application.account

import com.papertrading.api.application.account.port.BrokerAccountBalancePort
import com.papertrading.api.application.account.port.BrokerAccountRef
import com.papertrading.api.application.account.port.BrokerBalancePosition
import com.papertrading.api.application.account.port.BrokerBalanceSnapshot
import com.papertrading.api.application.account.port.BrokerCurrencyBalance
import com.papertrading.api.common.exception.AccountNotFoundException
import com.papertrading.api.common.exception.InternalErrorException
import com.papertrading.api.common.exception.InvalidAccountIdException
import com.papertrading.api.common.exception.KisAccountNotLinkedException
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.KisAccountMode
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.infrastructure.persistence.PositionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

// devkit:test-change D22 D23 DEC-05 A — 옛 KisAccountBalancePort.fetchBalance(accountId, trId) 대신 공통 잔고 포트와 ref 모드로 단언
class KisAccountQueryServiceTest {

    private val accountQueryService = mockk<AccountQueryService>()
    private val kisPort = mockk<BrokerAccountBalancePort> {
        every { supportedModes } returns setOf(TradingMode.KIS_PAPER, TradingMode.KIS_LIVE)
    }
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
        service = KisAccountQueryService(accountQueryService, listOf(kisPort), positionRepository)
        givenAccount(TradingMode.KIS_PAPER)
        every { positionRepository.findByAccountIdAndQuantityGreaterThan(any(), BigDecimal.ZERO) } returns emptyList()
    }

    private fun givenAccount(mode: TradingMode, externalAccountId: String? = ACCOUNT_NUMBER) {
        every { accountQueryService.getAccount(any()) } returns Account.create(
            accountName = "kis",
            accountType = AccountType.STOCK,
            tradingMode = mode,
            initialDeposit = BigDecimal.ZERO,
            externalAccountId = externalAccountId,
        )
    }

    @Test
    fun `LIVE 모드면 KIS_LIVE ref 로 포트를 부른다`() {
        every { kisPort.fetchBalance(any()) } returns snapshot()

        service.getBalance(1L, KisAccountMode.LIVE)

        verify(exactly = 1) { kisPort.fetchBalance(BrokerAccountRef(1L, TradingMode.KIS_LIVE, ACCOUNT_NUMBER)) }
    }

    @Test
    fun `PAPER 모드면 KIS_PAPER ref 로 포트를 부른다`() {
        every { kisPort.fetchBalance(any()) } returns snapshot()

        service.getBalance(1L, KisAccountMode.PAPER)

        verify(exactly = 1) { kisPort.fetchBalance(BrokerAccountRef(1L, TradingMode.KIS_PAPER, ACCOUNT_NUMBER)) }
    }

    // 가정 2: 모드는 계좌의 tradingMode 가 아니라 mode 파라미터가 정함
    @Test
    fun `KIS_PAPER 계좌를 mode=LIVE 로 조회해도 거부하지 않고 KIS_LIVE ref 로 부른다`() {
        givenAccount(TradingMode.KIS_PAPER)
        every { kisPort.fetchBalance(any()) } returns snapshot()

        val result = service.getBalance(1L, KisAccountMode.LIVE)

        assertThat(result.mode).isEqualTo(KisAccountMode.LIVE)
        verify(exactly = 1) { kisPort.fetchBalance(BrokerAccountRef(1L, TradingMode.KIS_LIVE, ACCOUNT_NUMBER)) }
    }

    @Test
    fun `BigDecimal 기반으로 요약과 포지션을 매핑한다`() {
        every { kisPort.fetchBalance(any()) } returns snapshot()

        val result = service.getBalance(7L, KisAccountMode.PAPER)

        assertThat(result.cashBalance).isEqualByComparingTo("1000000.01")
        assertThat(result.marketValue).isEqualByComparingTo("2000000.02")
        assertThat(result.unrealizedPnl).isEqualByComparingTo("10000.11")
        assertThat(result.returnRate).isEqualByComparingTo("1.23")
        assertThat(result.positions).hasSize(1)
        assertThat(result.positions[0].quantity).isEqualByComparingTo("3.5")
        assertThat(result.positions[0].ticker).isEqualTo("005930")
    }

    @Test
    fun `KST 조회 시각은 같은 순간의 UTC 표기로 내보낸다`() {
        every { kisPort.fetchBalance(any()) } returns snapshot()

        val result = service.getBalance(1L, KisAccountMode.PAPER)

        assertThat(result.asOf).isEqualTo(OffsetDateTime.parse("2026-05-02T01:00:00Z"))
        assertThat(result.asOf.offset).isEqualTo(ZoneOffset.UTC)
    }

    @Test
    fun `정합성 mismatch를 계산한다`() {
        every { kisPort.fetchBalance(any()) } returns snapshot(
            positions = listOf(
                BrokerBalancePosition(
                    ticker = "005930",
                    currency = "KRW",
                    quantity = BigDecimal("3"),
                    avgPrice = BigDecimal("70000"),
                    currentPrice = BigDecimal("71000"),
                    marketValue = BigDecimal("213000"),
                    unrealizedPnl = BigDecimal("3000"),
                    returnRate = BigDecimal("4.28"),
                ),
            ),
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
            ),
        )

        val result = service.getBalance(1L, KisAccountMode.LIVE)

        assertThat(result.reconciliation.quantityMismatch).containsExactly("005930")
        assertThat(result.reconciliation.missingInKis).containsExactly("000660")
        assertThat(result.reconciliation.missingInLocal).isEmpty()
    }

    @Test
    fun `계좌 id 가 0 이하면 계좌를 읽지 않고 InvalidAccountIdException 이다`() {
        assertThrows<InvalidAccountIdException> { service.getBalance(0L, KisAccountMode.LIVE) }

        verify(exactly = 0) { accountQueryService.getAccount(any()) }
        verify(exactly = 0) { kisPort.fetchBalance(any()) }
    }

    @Test
    fun `없는 계좌는 ACCOUNT_NOT_FOUND 이고 포트를 부르지 않는다`() {
        every { accountQueryService.getAccount(9L) } throws AccountNotFoundException(9L)

        assertThrows<AccountNotFoundException> { service.getBalance(9L, KisAccountMode.LIVE) }

        verify(exactly = 0) { kisPort.fetchBalance(any()) }
    }

    @Test
    fun `externalAccountId 가 없거나 형식이 틀리면 KIS_ACCOUNT_NOT_LINKED 이고 포트를 부르지 않는다`() {
        listOf(null, "", "99998888", "12345678-").forEach { value ->
            givenAccount(TradingMode.KIS_PAPER, value)

            assertThrows<KisAccountNotLinkedException> { service.getBalance(1L, KisAccountMode.PAPER) }
        }
        verify(exactly = 0) { kisPort.fetchBalance(any()) }
    }

    @Test
    fun `KRW 합계 행이 없는 스냅샷은 대체값 없이 내부 오류이다`() {
        every { kisPort.fetchBalance(any()) } returns snapshot().copy(currencyBalances = emptyList())

        assertThrows<InternalErrorException> { service.getBalance(1L, KisAccountMode.PAPER) }
    }

    @Test
    fun `KRW 합계 행의 금액이 null 이면 대체값 없이 내부 오류이다`() {
        val krw = snapshot().currencyBalances.single()
        every { kisPort.fetchBalance(any()) } returns
            snapshot().copy(currencyBalances = listOf(krw.copy(cashBalance = null)))

        assertThrows<InternalErrorException> { service.getBalance(1L, KisAccountMode.PAPER) }
    }

    private fun snapshot(
        positions: List<BrokerBalancePosition> = listOf(
            BrokerBalancePosition(
                ticker = "005930",
                currency = "KRW",
                quantity = BigDecimal("3.5"),
                avgPrice = BigDecimal("70000.00"),
                currentPrice = BigDecimal("71000.00"),
                marketValue = BigDecimal("248500.00"),
                unrealizedPnl = BigDecimal("3500.00"),
                returnRate = BigDecimal("1.43"),
            ),
        ),
    ) = BrokerBalanceSnapshot(
        asOf = LocalDateTime.of(2026, 5, 2, 10, 0),
        currencyBalances = listOf(
            BrokerCurrencyBalance(
                currency = "KRW",
                cashBalance = BigDecimal("1000000.01"),
                cashBuyingPower = null,
                marketValue = BigDecimal("2000000.02"),
                unrealizedPnl = BigDecimal("10000.11"),
                returnRate = BigDecimal("1.23"),
            ),
        ),
        positions = positions,
    )

    private companion object {
        const val ACCOUNT_NUMBER = "12345678-01"
    }
}
