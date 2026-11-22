package com.papertrading.api.application.order

import com.papertrading.api.application.notification.SlackNotificationEventPublisher
import com.papertrading.api.application.order.port.BrokerOrderPort
import com.papertrading.api.application.order.port.BrokerOrderReceipt
import com.papertrading.api.common.exception.BrokerOrderNotSupportedException
import com.papertrading.api.common.exception.InternalErrorException
import com.papertrading.api.common.exception.KisOrderRejectedException
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.support.LogCapture
import com.papertrading.api.support.withId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

// 브로커 주문 접수 선택: 모드를 전부 표로 (D25, D26, RULE-03)
class BrokerOrderSubmitterTest {

    private val slack = mockk<SlackNotificationEventPublisher>(relaxed = true)
    private val kisPort = portSupporting(TradingMode.KIS_PAPER, TradingMode.KIS_LIVE)
    private val submitter = BrokerOrderSubmitter(listOf(kisPort), slack)

    private fun portSupporting(vararg modes: TradingMode) = mockk<BrokerOrderPort> {
        every { supportedModes } returns modes.toSet()
    }

    private fun orderOf(mode: TradingMode): Order {
        val type = if (mode == TradingMode.UPBIT_LIVE) AccountType.CRYPTO else AccountType.STOCK
        val account = Account.create("시험 계좌", type, mode, BigDecimal("1000000"), externalAccountId = "12345678-01")
            .withId(1L)
        return Order.create(
            account = account,
            ticker = "005930",
            marketType = MarketType.KOSPI,
            orderType = OrderType.LIMIT,
            orderSide = OrderSide.BUY,
            orderCondition = OrderCondition.DAY,
            quantity = BigDecimal("5"),
            limitPrice = BigDecimal("70000"),
            lockedAmount = BigDecimal("350000"),
            idempotencyKey = "key-$mode",
        ).withId(10L)
    }

    private enum class Outcome { BROKER_PORT, NOT_SUPPORTED, NOT_BROKER_ORDER }

    // 새 모드를 넣으면 이 표에도 넣어야 시험이 통과함
    private val expectedByMode = mapOf(
        TradingMode.KIS_PAPER to Outcome.BROKER_PORT,
        TradingMode.KIS_LIVE to Outcome.BROKER_PORT,
        TradingMode.TOSS_LIVE to Outcome.NOT_SUPPORTED,
        TradingMode.UPBIT_LIVE to Outcome.NOT_SUPPORTED,
        TradingMode.LOCAL to Outcome.NOT_BROKER_ORDER,
    )

    @Test
    fun `모드 표의 키 집합이 TradingMode 전체와 같다`() {
        assertEquals(TradingMode.entries.toSet(), expectedByMode.keys)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TradingMode::class)
    fun `모드마다 표의 결과대로 동작한다`(mode: TradingMode) {
        val order = orderOf(mode)
        every { kisPort.submit(any()) } returns BrokerOrderReceipt("0000117057")

        val outcome = runCatching { submitter.submit(order) }
        val error = outcome.exceptionOrNull()

        when (expectedByMode.getValue(mode)) {
            Outcome.BROKER_PORT -> {
                outcome.getOrThrow()
                verify(exactly = 1) { kisPort.submit(order) }
                assertEquals("0000117057", order.externalOrderId)
            }

            Outcome.NOT_SUPPORTED -> {
                assertTrue(error is BrokerOrderNotSupportedException, "$mode $error")
                verify(exactly = 0) { kisPort.submit(any()) }
            }

            Outcome.NOT_BROKER_ORDER -> {
                assertTrue(error is InternalErrorException, "$mode $error")
                verify(exactly = 0) { kisPort.submit(any()) }
            }
        }
    }

    // 특성화 시험(KisOrderIntakeBoundaryCharacterizationTest)이 단언하는 Slack 문구와 같은 글자
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = ["KIS_PAPER", "KIS_LIVE"])
    fun `접수 성공은 모드 이름으로 지금과 같은 Slack 문구와 접수 로그를 남긴다`(modeName: String) {
        val order = orderOf(TradingMode.valueOf(modeName))
        every { kisPort.submit(any()) } returns BrokerOrderReceipt("0000117057")

        LogCapture().use { logs ->
            submitter.submit(order)

            val infos = logs.linesOf("BrokerOrderSubmitter").filter { it.contains(" INFO ") }
            assertEquals(1, infos.size, infos.toString())
            assertTrue(
                infos[0].endsWith("$modeName 주문 접수: orderId=10, orno=0000117057, ticker=005930"),
                infos[0],
            )
        }
        verify(exactly = 1) {
            slack.publishOrderCreated(
                accountId = 1L,
                orderId = 10L,
                message = "$modeName order created: orderId=10, ticker=005930, externalOrderId=0000117057",
                occurredAt = any(),
            )
        }
    }

    @Test
    fun `포트 실패는 바꾸지 않고 그대로 올리고 주문번호와 Slack 을 남기지 않는다`() {
        val order = orderOf(TradingMode.KIS_LIVE)
        val failure = KisOrderRejectedException(RuntimeException("KIS-원문"))
        every { kisPort.submit(any()) } throws failure

        LogCapture().use { logs ->
            val thrown = assertThrows<KisOrderRejectedException> { submitter.submit(order) }

            assertSame(failure, thrown)
            val errors = logs.linesOf("BrokerOrderSubmitter").filter { it.contains(" ERROR ") }
            assertEquals(1, errors.size, errors.toString())
            assertTrue(errors[0].contains("KIS_LIVE 주문 접수 실패: orderId=10, reason="), errors[0])
        }
        assertNull(order.externalOrderId)
        verify(exactly = 0) { slack.publishOrderCreated(any(), any(), any(), any()) }
    }

    @Test
    fun `어댑터로 보내는 모드에 구현이 없으면 생성 때 모드 이름과 함께 실패한다`() {
        val error = assertThrows<IllegalStateException> { BrokerOrderSubmitter(emptyList(), slack) }

        val message = error.message.orEmpty()
        assertTrue(message.contains("KIS_PAPER") && message.contains("KIS_LIVE"), message)
    }

    @Test
    fun `한 모드에 구현이 둘이면 생성 때 모드 이름과 함께 실패한다`() {
        val error = assertThrows<IllegalStateException> {
            BrokerOrderSubmitter(listOf(kisPort, portSupporting(TradingMode.KIS_LIVE)), slack)
        }

        assertTrue(error.message.orEmpty().contains("KIS_LIVE"), error.message)
    }

    @ParameterizedTest(name = "구현이 {0} 를 지원하면 생성 때 실패한다")
    @ValueSource(strings = ["TOSS_LIVE", "UPBIT_LIVE", "LOCAL"])
    fun `어댑터로 보내지 않는 모드를 구현이 지원하면 생성 때 모드 이름과 함께 실패한다`(modeName: String) {
        val error = assertThrows<IllegalStateException> {
            BrokerOrderSubmitter(listOf(kisPort, portSupporting(TradingMode.valueOf(modeName))), slack)
        }

        assertTrue(error.message.orEmpty().contains(modeName), error.message)
    }
}
