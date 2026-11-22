package com.papertrading.api.infrastructure.kis

import com.papertrading.api.common.exception.InternalErrorException
import com.papertrading.api.common.exception.KisAccountNotLinkedException
import com.papertrading.api.common.exception.KisAuthFailedException
import com.papertrading.api.common.exception.KisOrderRejectedException
import com.papertrading.api.common.exception.KisOrderResultUnknownException
import com.papertrading.api.common.exception.KisRateLimitedException
import com.papertrading.api.common.exception.KisTimeoutException
import com.papertrading.api.common.exception.KisUnavailableException
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.support.withId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.http.HttpConnectTimeoutException
import java.net.http.HttpTimeoutException

// KIS 주문 어댑터 (D26, D28): 실제 KisOrderRestClient 에 RestTemplate 만 가짜로 두어 KIS 응답 → 우리 예외 매핑을 끝까지 봄
class KisOrderAdapterTest {

    private val properties = KisOrderProperties().apply {
        paperRestBaseUrl = "https://paper.example"
        liveRestBaseUrl = "https://live.example"
        paperAppKey = "paper-key"
        paperAppSecret = "paper-secret"
        liveAppKey = "live-key"
        liveAppSecret = "live-secret"
    }
    private val tokenManager = mockk<KisTokenManager>()
    private val restTemplate = mockk<RestTemplate>()
    private val adapter = KisOrderAdapter(KisOrderRestClient(properties, tokenManager, restTemplate))

    private fun orderOf(mode: TradingMode, externalAccountId: String? = "12345678-01"): Order {
        val type = if (mode == TradingMode.UPBIT_LIVE) AccountType.CRYPTO else AccountType.STOCK
        val account = Account.create("시험 계좌", type, mode, BigDecimal("1000000"), externalAccountId = externalAccountId)
            .withId(1L)
        return Order.create(
            account = account,
            ticker = "005930",
            marketType = MarketType.KOSPI,
            orderType = OrderType.LIMIT,
            orderSide = OrderSide.BUY,
            orderCondition = OrderCondition.DAY,
            quantity = BigDecimal("1"),
            limitPrice = BigDecimal("70000"),
            lockedAmount = BigDecimal("70000"),
            idempotencyKey = "key",
        ).withId(10L)
    }

    private fun stubResponse(body: Map<String, Any>) {
        every { tokenManager.getToken(any()) } returns "token"
        every { restTemplate.exchange(any<String>(), HttpMethod.POST, any(), Map::class.java) } returns
            ResponseEntity.ok(body)
    }

    private fun stubTransportFailure(failure: RestClientException) {
        every { tokenManager.getToken(any()) } returns "token"
        every { restTemplate.exchange(any<String>(), HttpMethod.POST, any(), Map::class.java) } throws failure
    }

    private fun verifyNoKisCall() {
        verify(exactly = 0) { tokenManager.getToken(any()) }
        verify(exactly = 0) { restTemplate.exchange(any<String>(), any<HttpMethod>(), any(), Map::class.java) }
    }

    @Test
    fun `지원 모드는 KIS_PAPER 와 KIS_LIVE 이다`() {
        assertThat(adapter.supportedModes).containsExactlyInAnyOrder(TradingMode.KIS_PAPER, TradingMode.KIS_LIVE)
    }

    @Test
    fun `KIS_PAPER 는 paper 주소·키와 VTTC0802U 로 접수하고 주문번호를 돌려준다`() {
        val url = slot<String>()
        val entity = slot<HttpEntity<*>>()
        every { tokenManager.getToken("paper") } returns "paper-token"
        every { restTemplate.exchange(capture(url), HttpMethod.POST, capture(entity), Map::class.java) } returns
            ResponseEntity.ok(mapOf("rt_cd" to "0", "output" to mapOf("ORNO" to "0000117057")))

        val receipt = adapter.submit(orderOf(TradingMode.KIS_PAPER))

        assertThat(receipt.externalOrderId).isEqualTo("0000117057")
        assertThat(url.captured).startsWith("https://paper.example/")
        assertThat(entity.captured.headers.getFirst("tr_id")).isEqualTo("VTTC0802U")
        assertThat(entity.captured.headers.getFirst("appkey")).isEqualTo("paper-key")
    }

    @Test
    fun `KIS_LIVE 는 live 주소·키와 TTTC0802U 로 접수한다`() {
        val url = slot<String>()
        val entity = slot<HttpEntity<*>>()
        every { tokenManager.getToken("live") } returns "live-token"
        every { restTemplate.exchange(capture(url), HttpMethod.POST, capture(entity), Map::class.java) } returns
            ResponseEntity.ok(mapOf("rt_cd" to "0", "output" to mapOf("ORNO" to "0000228899")))

        val receipt = adapter.submit(orderOf(TradingMode.KIS_LIVE))

        assertThat(receipt.externalOrderId).isEqualTo("0000228899")
        assertThat(url.captured).startsWith("https://live.example/")
        assertThat(entity.captured.headers.getFirst("tr_id")).isEqualTo("TTTC0802U")
        assertThat(entity.captured.headers.getFirst("appkey")).isEqualTo("live-key")
    }

    @Test
    fun `KIS 가 아닌 모드는 내부 오류이고 토큰도 KIS 호출도 0회이다`() {
        listOf(TradingMode.LOCAL, TradingMode.UPBIT_LIVE, TradingMode.TOSS_LIVE).forEach { mode ->
            val ex = assertThrows(InternalErrorException::class.java) { adapter.submit(orderOf(mode)) }

            assertThat(ex.message).contains(mode.name).doesNotContain("12345678")
        }
        verifyNoKisCall()
    }

    @ParameterizedTest(name = "externalAccountId={0}")
    @MethodSource("unlinkedAccountNumbers")
    fun `계좌번호가 없거나 XXXXXXXX-XX 가 아니면 KIS 호출 없이 KIS_ACCOUNT_NOT_LINKED 이고 값을 싣지 않는다`(
        externalAccountId: String?,
    ) {
        val ex = assertThrows(KisAccountNotLinkedException::class.java) {
            adapter.submit(orderOf(TradingMode.KIS_PAPER, externalAccountId))
        }

        assertThat(ex.errorCode).isEqualTo("KIS_ACCOUNT_NOT_LINKED")
        assertThat(ex.status.value()).isEqualTo(409)
        externalAccountId?.takeIf { it.isNotBlank() }?.let { assertThat(ex.message).doesNotContain(it) }
        verifyNoKisCall()
    }

    // 주문 거부의 흔한 rt_cd 는 1, rt_cd 만으로 인증이라 보지 않음 (S9-02)
    @ParameterizedTest(name = "rt_cd={0} msg1={1} → {2}")
    @MethodSource("responseFailures")
    fun `KIS 응답 실패는 원인별 우리 예외로 바꾸고 KIS 원문은 cause 에만 둔다`(
        rtCd: String,
        msg1: String,
        expected: Class<out RuntimeException>,
        expectedCode: String,
    ) {
        stubResponse(mapOf("rt_cd" to rtCd, "msg1" to msg1))

        val ex = assertThrows(expected) { adapter.submit(orderOf(TradingMode.KIS_PAPER)) }

        assertThat(errorCodeOf(ex)).isEqualTo(expectedCode)
        assertThat(ex.message).doesNotContain(msg1).doesNotContain("12345678")
        assertThat(ex.cause).isInstanceOf(KisApiException::class.java)
        assertThat(ex.cause?.message).contains(msg1)
    }

    // devkit:test-change 사람 결정 2026-11-21 — HTTP 200 인데 rt_cd 없음·본문 없음은 접수 여부를 몰라 결과 미확인
    @Test
    fun `rt_cd 가 없는 200 응답은 문구와 무관하게 KIS_ORDER_RESULT_UNKNOWN 이다`() {
        listOf(emptyMap(), mapOf("msg1" to "계좌번호를 확인해 주세요")).forEach { body ->
            stubResponse(body)

            val ex = assertThrows(KisOrderResultUnknownException::class.java) {
                adapter.submit(orderOf(TradingMode.KIS_PAPER))
            }

            assertThat(ex.errorCode).isEqualTo("KIS_ORDER_RESULT_UNKNOWN")
            assertThat(ex.cause).isInstanceOf(KisApiException::class.java)
        }
    }

    @Test
    fun `본문이 없는 200 응답은 KIS_ORDER_RESULT_UNKNOWN 이다`() {
        every { tokenManager.getToken(any()) } returns "token"
        every { restTemplate.exchange(any<String>(), HttpMethod.POST, any(), Map::class.java) } returns
            ResponseEntity.ok().build()

        val ex = assertThrows(KisOrderResultUnknownException::class.java) {
            adapter.submit(orderOf(TradingMode.KIS_PAPER))
        }

        assertThat(ex.errorCode).isEqualTo("KIS_ORDER_RESULT_UNKNOWN")
    }

    // devkit:test-change C-01 step 11 리뷰 — KIS 가 접수를 확정한 rt_cd 0 인데 주문번호가 없으면 재시도 안내 대신 결과 미확인
    @Test
    fun `ORNO 없는 성공 응답은 KIS_ORDER_RESULT_UNKNOWN 이다`() {
        stubResponse(mapOf("rt_cd" to "0", "output" to emptyMap<String, String>()))

        val ex = assertThrows(KisOrderResultUnknownException::class.java) {
            adapter.submit(orderOf(TradingMode.KIS_PAPER))
        }

        assertThat(ex.errorCode).isEqualTo("KIS_ORDER_RESULT_UNKNOWN")
    }

    // C-02: KIS 오류 본문이 HTTP 4xx·5xx 로 와도 본문으로 확정 실패 분류
    @ParameterizedTest(name = "HTTP {0} 본문={1} → {2}")
    @MethodSource("httpErrorFailures")
    fun `HTTP 오류 응답은 KIS 본문이 있으면 본문으로 없으면 상태로 분류한다`(
        status: Int,
        body: String,
        expected: Class<out RuntimeException>,
    ) {
        stubTransportFailure(httpError(status, body))

        assertThrows(expected) { adapter.submit(orderOf(TradingMode.KIS_PAPER)) }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("transportFailures")
    fun `요청을 못 보낸 게 확실한 전송 실패만 KIS_UNAVAILABLE 이고 나머지는 결과 미확인이다`(
        failure: RestClientException,
        expected: Class<out RuntimeException>,
    ) {
        stubTransportFailure(failure)

        val ex = assertThrows(expected) { adapter.submit(orderOf(TradingMode.KIS_PAPER)) }

        assertThat(ex.cause).isSameAs(failure)
    }

    private fun errorCodeOf(ex: RuntimeException): String = when (ex) {
        is KisOrderRejectedException -> ex.errorCode
        is KisAuthFailedException -> ex.errorCode
        is KisRateLimitedException -> ex.errorCode
        is KisUnavailableException -> ex.errorCode
        else -> "unexpected ${ex::class.simpleName}"
    }

    companion object {
        @JvmStatic
        fun unlinkedAccountNumbers(): List<String?> = listOf(null, "", "12345678", "12345678-", "-01", "1234-5678-01")

        @JvmStatic
        fun responseFailures(): List<Arguments> = listOf(
            Arguments.of("1", "모의투자 장시작전 입니다.", KisOrderRejectedException::class.java, "KIS_ORDER_REJECTED"),
            Arguments.of("1", "주문가능금액을 초과 했습니다", KisOrderRejectedException::class.java, "KIS_ORDER_REJECTED"),
            Arguments.of("1", "계좌번호를 확인해 주세요", KisOrderRejectedException::class.java, "KIS_ORDER_REJECTED"),
            Arguments.of("1", "중복 주문입니다", KisOrderRejectedException::class.java, "KIS_ORDER_REJECTED"),
            Arguments.of("1", "기간이 만료된 token 입니다.", KisAuthFailedException::class.java, "KIS_AUTH_FAILED"),
            Arguments.of("1", "초당 거래건수를 초과하였습니다.", KisRateLimitedException::class.java, "KIS_RATE_LIMITED"),
        )

        @JvmStatic
        fun transportFailures(): List<Arguments> = listOf(
            Arguments.of(
                ResourceAccessException("I/O error", SocketTimeoutException("Read timed out")),
                KisOrderResultUnknownException::class.java,
            ),
            Arguments.of(
                ResourceAccessException("I/O error", HttpTimeoutException("request timed out")),
                KisOrderResultUnknownException::class.java,
            ),
            Arguments.of(
                ResourceAccessException("I/O error", HttpConnectTimeoutException("connect timed out")),
                KisUnavailableException::class.java,
            ),
            Arguments.of(
                ResourceAccessException("I/O error", ConnectException("Connection refused")),
                KisUnavailableException::class.java,
            ),
            Arguments.of(
                ResourceAccessException("I/O error", UnknownHostException("paper.example")),
                KisUnavailableException::class.java,
            ),
            Arguments.of(RestClientException("응답 변환 실패"), KisOrderResultUnknownException::class.java),
        )

        @JvmStatic
        fun httpErrorFailures(): List<Arguments> = listOf(
            Arguments.of(500, "", KisOrderResultUnknownException::class.java),
            Arguments.of(502, "<html>Bad Gateway</html>", KisOrderResultUnknownException::class.java),
            Arguments.of(400, "", KisUnavailableException::class.java),
            Arguments.of(500, kisBody("1", "EGW00201", "초당 거래건수를 초과하였습니다."), KisRateLimitedException::class.java),
            Arguments.of(400, kisBody("1", "APBK0919", "모의투자 장시작전 입니다."), KisOrderRejectedException::class.java),
            Arguments.of(500, kisBody("1", "EGW00123", "기간이 만료된 token 입니다."), KisAuthFailedException::class.java),
            // devkit:test-change R1-02 step 11 재리뷰 — 5xx 본문은 한도·인증만 확정, 나머지 rt_cd≠0 은 결과 미확인
            Arguments.of(500, kisBody("1", "EGW00002", "서버 에러가 발생 했습니다."), KisOrderResultUnknownException::class.java),
            Arguments.of(500, kisBody("1", "APBK0919", "모의투자 장시작전 입니다."), KisOrderResultUnknownException::class.java),
            Arguments.of(500, kisBody("1", "APBK0952", "주문가능금액을 초과 했습니다"), KisOrderResultUnknownException::class.java),
            Arguments.of(503, kisBody("1", "XXXX0000", "인증 정보가 올바르지 않습니다"), KisAuthFailedException::class.java),
        )
    }
}
