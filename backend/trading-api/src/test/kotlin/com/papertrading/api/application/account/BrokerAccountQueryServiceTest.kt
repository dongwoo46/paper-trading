package com.papertrading.api.application.account

import com.papertrading.api.application.account.port.BrokerAccountBalancePort
import com.papertrading.api.application.account.port.BrokerAccountRef
import com.papertrading.api.application.account.port.BrokerBalancePosition
import com.papertrading.api.application.account.port.BrokerBalanceSnapshot
import com.papertrading.api.application.account.port.BrokerCurrencyBalance
import com.papertrading.api.common.exception.AccountNotFoundException
import com.papertrading.api.common.exception.BrokerBalanceNotSupportedException
import com.papertrading.api.common.exception.ConflictException
import com.papertrading.api.common.exception.KisAccountNotLinkedException
import com.papertrading.api.common.exception.KisTimeoutException
import com.papertrading.api.common.exception.NotBrokerAccountException
import com.papertrading.api.common.exception.NotFoundException
import com.papertrading.api.common.exception.NotImplementedException
import com.papertrading.api.common.exception.TossAccountNotLinkedException
import com.papertrading.api.common.exception.TossResponseInvalidException
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.enums.AccountSource
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TradingMode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.time.LocalDateTime

// 계좌 모드별 잔고 조회 선택: 모드를 전부 표로 (D25, RULE-03)
class BrokerAccountQueryServiceTest {

    private val accountQueryService = mockk<AccountQueryService>()
    private val tossPort = portSupporting(TradingMode.TOSS_LIVE)
    private val kisPort = portSupporting(TradingMode.KIS_PAPER, TradingMode.KIS_LIVE)
    private val service = BrokerAccountQueryService(accountQueryService, listOf(tossPort, kisPort))

    private fun portSupporting(vararg modes: TradingMode) = mockk<BrokerAccountBalancePort> {
        every { supportedModes } returns modes.toSet()
    }

    private val linkedSeq = "1234567890"
    private val kisAccountNumber = "12345678-01"
    private val snapshot = BrokerBalanceSnapshot(
        asOf = LocalDateTime.of(2026, 11, 15, 10, 0),
        currencyBalances = listOf(
            BrokerCurrencyBalance(
                currency = "KRW",
                cashBalance = null,
                cashBuyingPower = BigDecimal("5000000"),
                marketValue = BigDecimal("7200000"),
                unrealizedPnl = BigDecimal("700000"),
                returnRate = BigDecimal("10.77"),
            ),
        ),
        positions = listOf(
            BrokerBalancePosition(
                ticker = "005930",
                currency = "KRW",
                quantity = BigDecimal("100"),
                avgPrice = BigDecimal("65000"),
                currentPrice = BigDecimal("72000"),
                marketValue = BigDecimal("7200000"),
                unrealizedPnl = BigDecimal("700000"),
                returnRate = BigDecimal("10.77"),
            ),
        ),
    )

    private fun accountOf(mode: TradingMode, externalAccountId: String? = linkedSeq): Account {
        val type = if (mode == TradingMode.UPBIT_LIVE) AccountType.CRYPTO else AccountType.STOCK
        return Account.create("시험 계좌", type, mode, BigDecimal.ZERO, externalAccountId = externalAccountId)
    }

    private fun givenAccount(mode: TradingMode, externalAccountId: String? = linkedSeq) {
        every { accountQueryService.getAccount(1L) } returns accountOf(mode, externalAccountId)
    }

    private enum class Outcome { TOSS_ADAPTER, KIS_ADAPTER, NOT_SUPPORTED, NOT_BROKER_ACCOUNT }

    // 새 모드를 넣으면 이 표에도 넣어야 시험이 통과함
    private val expectedByMode = mapOf(
        TradingMode.TOSS_LIVE to Outcome.TOSS_ADAPTER,
        TradingMode.KIS_PAPER to Outcome.KIS_ADAPTER,
        TradingMode.KIS_LIVE to Outcome.KIS_ADAPTER,
        TradingMode.UPBIT_LIVE to Outcome.NOT_SUPPORTED,
        TradingMode.LOCAL to Outcome.NOT_BROKER_ACCOUNT,
    )

    private fun externalAccountIdFor(mode: TradingMode): String {
        val isKis = mode == TradingMode.KIS_PAPER || mode == TradingMode.KIS_LIVE
        return if (isKis) kisAccountNumber else linkedSeq
    }

    @Test
    fun `모드 표의 키 집합이 TradingMode 전체와 같다`() {
        assertEquals(TradingMode.entries.toSet(), expectedByMode.keys)
    }

    @Test
    fun `모드마다 표의 결과대로 동작한다`() {
        TradingMode.entries.forEach { mode ->
            givenAccount(mode, externalAccountIdFor(mode))
            every { tossPort.fetchBalance(any()) } returns snapshot
            every { kisPort.fetchBalance(any()) } returns snapshot

            val outcome = runCatching { service.getBalance(1L) }
            val error = outcome.exceptionOrNull()

            when (expectedByMode.getValue(mode)) {
                Outcome.TOSS_ADAPTER -> assertEquals(AccountSource.TOSS, outcome.getOrThrow().source, "$mode")

                Outcome.KIS_ADAPTER -> {
                    assertEquals(AccountSource.KIS, outcome.getOrThrow().source, "$mode")
                    assertEquals(mode, outcome.getOrThrow().tradingMode, "$mode")
                    verify(exactly = 1) { kisPort.fetchBalance(BrokerAccountRef(1L, mode, kisAccountNumber)) }
                }

                Outcome.NOT_SUPPORTED -> assertTrue(error is BrokerBalanceNotSupportedException, "$mode $error")

                Outcome.NOT_BROKER_ACCOUNT -> assertTrue(error is NotBrokerAccountException, "$mode $error")
            }
        }
    }

    @Test
    fun `TOSS_LIVE 는 토스 어댑터 결과를 계좌 식별값과 출처와 함께 돌려주고 어댑터에 accountSeq 를 넘긴다`() {
        givenAccount(TradingMode.TOSS_LIVE)
        every { tossPort.fetchBalance(any()) } returns snapshot

        val result = service.getBalance(1L)

        assertEquals(1L, result.accountId)
        assertEquals(AccountSource.TOSS, result.source)
        assertEquals(TradingMode.TOSS_LIVE, result.tradingMode)
        assertEquals(snapshot.asOf, result.asOf)
        assertEquals(snapshot.currencyBalances, result.currencyBalances)
        assertEquals(snapshot.positions, result.positions)
        verify(exactly = 1) { tossPort.fetchBalance(BrokerAccountRef(1L, TradingMode.TOSS_LIVE, linkedSeq)) }
    }

    @Test
    fun `UPBIT_LIVE 계좌는 어댑터를 부르지 않고 BROKER_BALANCE_NOT_SUPPORTED 이다`() {
        givenAccount(TradingMode.UPBIT_LIVE)

        val error = assertThrows<BrokerBalanceNotSupportedException> { service.getBalance(1L) }

        assertEquals("BROKER_BALANCE_NOT_SUPPORTED", error.errorCode)
        assertTrue(error.message.orEmpty().contains("UPBIT_LIVE"), error.message)
        verify(exactly = 0) { tossPort.fetchBalance(any()) }
        verify(exactly = 0) { kisPort.fetchBalance(any()) }
    }

    @Test
    fun `LOCAL 계좌는 브로커 계좌가 아니라 NOT_BROKER_ACCOUNT 이고 어댑터를 부르지 않는다`() {
        givenAccount(TradingMode.LOCAL)

        val error = assertThrows<NotBrokerAccountException> { service.getBalance(1L) }

        assertEquals("NOT_BROKER_ACCOUNT", error.errorCode)
        verify(exactly = 0) { tossPort.fetchBalance(any()) }
        verify(exactly = 0) { kisPort.fetchBalance(any()) }
    }

    @ParameterizedTest(name = "externalAccountId [{0}] 는 미연결이다")
    @ValueSource(strings = ["", "   ", "abc", "123-45", "12 34", "-1", "+1", "１２３", "99999999999999999999"])
    fun `TOSS 계좌의 externalAccountId 가 비었거나 숫자가 아니면 TOSS_ACCOUNT_NOT_LINKED 이고 어댑터를 부르지 않는다`(value: String) {
        givenAccount(TradingMode.TOSS_LIVE, value)

        val error = assertThrows<TossAccountNotLinkedException> { service.getBalance(1L) }

        assertEquals("TOSS_ACCOUNT_NOT_LINKED", error.errorCode)
        assertFalse(error.message.orEmpty().contains(value.trim().ifEmpty { "\u0000" }), "값은 싣지 않음: ${error.message}")
        verify(exactly = 0) { tossPort.fetchBalance(any()) }
    }

    @Test
    fun `TOSS 계좌의 externalAccountId 가 null 이면 TOSS_ACCOUNT_NOT_LINKED 이다`() {
        givenAccount(TradingMode.TOSS_LIVE, null)

        assertThrows<TossAccountNotLinkedException> { service.getBalance(1L) }

        verify(exactly = 0) { tossPort.fetchBalance(any()) }
    }

    @Test
    fun `externalAccountId 앞뒤 공백은 지우고 숫자 전체를 그대로 넘긴다`() {
        givenAccount(TradingMode.TOSS_LIVE, " 9876543210 ")
        every { tossPort.fetchBalance(any()) } returns snapshot

        service.getBalance(1L)

        verify(exactly = 1) { tossPort.fetchBalance(BrokerAccountRef(1L, TradingMode.TOSS_LIVE, "9876543210")) }
    }

    @Test
    fun `LOCAL 과 UPBIT_LIVE 계좌는 externalAccountId 검사보다 모드 판정이 먼저이다`() {
        givenAccount(TradingMode.UPBIT_LIVE, "KIS-ACCOUNT")
        assertThrows<BrokerBalanceNotSupportedException> { service.getBalance(1L) }

        givenAccount(TradingMode.LOCAL, null)
        assertThrows<NotBrokerAccountException> { service.getBalance(1L) }
    }

    // devkit:test-change D24 — KIS 두 모드가 501 에서 KIS 어댑터 조회로 바뀐 기준, 옛 «KIS_PAPER·KIS_LIVE 501» 단언을 대체
    @ParameterizedTest(name = "KIS 계좌의 externalAccountId [{0}] 는 미연결이다")
    @ValueSource(strings = ["", "   ", "abc", "99998888", "1234-5678-01", "12345678-", "-01", "12345678- "])
    fun `KIS 계좌의 externalAccountId 가 XXXXXXXX-XX 형식이 아니면 KIS_ACCOUNT_NOT_LINKED 이고 어댑터를 부르지 않는다`(value: String) {
        TradingMode.entries.filter { it == TradingMode.KIS_PAPER || it == TradingMode.KIS_LIVE }.forEach { mode ->
            givenAccount(mode, value)

            val error = assertThrows<KisAccountNotLinkedException> { service.getBalance(1L) }

            assertEquals("KIS_ACCOUNT_NOT_LINKED", error.errorCode)
            val leaked = error.message.orEmpty().contains(value.trim().ifEmpty { "\u0000" })
            assertFalse(leaked, "값은 싣지 않음: ${error.message}")
        }
        verify(exactly = 0) { kisPort.fetchBalance(any()) }
    }

    @Test
    fun `KIS 계좌의 externalAccountId 가 null 이면 KIS_ACCOUNT_NOT_LINKED 이다`() {
        givenAccount(TradingMode.KIS_LIVE, null)

        assertThrows<KisAccountNotLinkedException> { service.getBalance(1L) }

        verify(exactly = 0) { kisPort.fetchBalance(any()) }
    }

    @Test
    fun `KIS 어댑터 결과를 계좌 식별값과 KIS 출처와 함께 돌려주고 어댑터 예외는 그대로 올린다`() {
        givenAccount(TradingMode.KIS_PAPER, kisAccountNumber)
        every { kisPort.fetchBalance(any()) } returns snapshot

        val result = service.getBalance(1L)

        assertEquals(1L, result.accountId)
        assertEquals(AccountSource.KIS, result.source)
        assertEquals(TradingMode.KIS_PAPER, result.tradingMode)
        assertEquals(snapshot.asOf, result.asOf)
        assertEquals(snapshot.currencyBalances, result.currencyBalances)
        assertEquals(snapshot.positions, result.positions)
        verify(exactly = 0) { tossPort.fetchBalance(any()) }

        every { kisPort.fetchBalance(any()) } throws KisTimeoutException(null)
        assertThrows<KisTimeoutException> { service.getBalance(1L) }
    }

    // devkit:test-change D25 — 선택을 매개변수 이름이 아니라 supportedModes 표와 생성 때 검사로 바꾼 기준
    @Test
    fun `어댑터로 보내는 모드에 구현이 없으면 생성 때 모드 이름과 함께 실패한다`() {
        val error = assertThrows<IllegalStateException> {
            BrokerAccountQueryService(accountQueryService, listOf(tossPort))
        }

        val message = error.message.orEmpty()
        assertTrue(message.contains("KIS_PAPER") && message.contains("KIS_LIVE"), message)
    }

    @Test
    fun `한 모드에 구현이 둘이면 생성 때 모드 이름과 함께 실패한다`() {
        val error = assertThrows<IllegalStateException> {
            BrokerAccountQueryService(
                accountQueryService,
                listOf(tossPort, kisPort, portSupporting(TradingMode.TOSS_LIVE)),
            )
        }

        assertTrue(error.message.orEmpty().contains("TOSS_LIVE"), error.message)
    }

    @ParameterizedTest(name = "구현이 {0} 를 지원하면 생성 때 실패한다")
    @ValueSource(strings = ["UPBIT_LIVE", "LOCAL"])
    fun `어댑터로 보내지 않는 모드를 구현이 지원하면 생성 때 모드 이름과 함께 실패한다`(modeName: String) {
        val error = assertThrows<IllegalStateException> {
            BrokerAccountQueryService(
                accountQueryService,
                listOf(tossPort, kisPort, portSupporting(TradingMode.valueOf(modeName))),
            )
        }

        assertTrue(error.message.orEmpty().contains(modeName), error.message)
    }

    @Test
    fun `없는 계좌는 ACCOUNT_NOT_FOUND 이고 어댑터를 부르지 않는다`() {
        every { accountQueryService.getAccount(1L) } throws AccountNotFoundException(1L)

        val error = assertThrows<AccountNotFoundException> { service.getBalance(1L) }

        assertEquals("ACCOUNT_NOT_FOUND", error.errorCode)
        verify(exactly = 0) { tossPort.fetchBalance(any()) }
    }

    @Test
    fun `어댑터 예외는 바꾸지 않고 그대로 올린다`() {
        givenAccount(TradingMode.TOSS_LIVE)
        every { tossPort.fetchBalance(any()) } throws TossResponseInvalidException("필드 경로=items")

        assertThrows<TossResponseInvalidException> { service.getBalance(1L) }
    }

    @Test
    fun `새 예외는 범주 기반 부모를 상속한다`() {
        assertTrue(NotImplementedException::class.java.isAssignableFrom(BrokerBalanceNotSupportedException::class.java))
        assertTrue(NotFoundException::class.java.isAssignableFrom(NotBrokerAccountException::class.java))
        assertTrue(ConflictException::class.java.isAssignableFrom(TossAccountNotLinkedException::class.java))
    }

    @Test
    fun `BrokerAccountRef 문자열 표현은 accountSeq 를 끝 4자리만 보인다`() {
        val text = BrokerAccountRef(1L, TradingMode.TOSS_LIVE, linkedSeq).toString()

        assertFalse(text.contains(linkedSeq), text)
        assertTrue(text.contains("7890"), text)
    }
}
