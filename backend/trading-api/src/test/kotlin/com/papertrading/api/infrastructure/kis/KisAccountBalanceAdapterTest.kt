package com.papertrading.api.infrastructure.kis

import com.papertrading.api.application.account.port.BrokerAccountRef
import com.papertrading.api.common.exception.InternalErrorException
import com.papertrading.api.common.exception.KisAccountNotLinkedException
import com.papertrading.api.common.exception.KisAuthFailedException
import com.papertrading.api.common.exception.KisPermissionDeniedException
import com.papertrading.api.common.exception.KisRateLimitedException
import com.papertrading.api.common.exception.KisTimeoutException
import com.papertrading.api.common.exception.KisUnavailableException
import com.papertrading.api.domain.enums.TradingMode
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

// devkit:test-change D22 DEC-05 A — fetchBalance(accountId, trId) 와 계좌 로드를 공통 포트(BrokerAccountRef)로 옮긴 기준
// 모드는 ref.tradingMode 가 정함
class KisAccountBalanceAdapterTest {

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
    private val fixedClock = Clock.fixed(Instant.parse("2026-11-16T01:00:00Z"), ZoneOffset.UTC)
    private val adapter = KisAccountBalanceAdapter(properties, tokenManager, restTemplate, fixedClock)

    private val paperRef = BrokerAccountRef(1L, TradingMode.KIS_PAPER, "12345678-01")
    private val liveRef = BrokerAccountRef(1L, TradingMode.KIS_LIVE, "12345678-01")

    @Test
    fun `지원 모드는 KIS_PAPER 와 KIS_LIVE 이다`() {
        assertThat(adapter.supportedModes).containsExactlyInAnyOrder(TradingMode.KIS_PAPER, TradingMode.KIS_LIVE)
    }

    @Test
    fun `KIS_PAPER 는 paper endpoint 와 VTTC8434R 로 호출하고 KRW 한 줄 스냅샷으로 매핑한다`() {
        val entity = slot<HttpEntity<*>>()
        every { tokenManager.getToken("paper") } returns "paper-token"
        every {
            restTemplate.exchange(
                match<String> {
                    it.startsWith("https://paper.example/") && it.contains("CANO=12345678&ACNT_PRDT_CD=01")
                },
                HttpMethod.GET,
                capture(entity),
                Map::class.java,
            )
        } returns ResponseEntity.ok(
            mapOf(
                "rt_cd" to "0",
                "output1" to listOf(
                    mapOf(
                        "pdno" to "005930",
                        "hldg_qty" to "3.5000",
                        "pchs_avg_pric" to "70000",
                        "prpr" to "71000",
                        "evlu_amt" to "248500",
                        "evlu_pfls_amt" to "3500",
                        "evlu_erng_rt" to "1.43",
                    ),
                ),
                "output2" to listOf(
                    mapOf(
                        "dnca_tot_amt" to "1000000.01",
                        "scts_evlu_amt" to "248500",
                        "evlu_pfls_smtl_amt" to "3500",
                        "evlu_erng_rt" to "1.43",
                    ),
                ),
            ),
        )

        val snapshot = adapter.fetchBalance(paperRef)

        assertThat(entity.captured.headers.getFirst("tr_id")).isEqualTo("VTTC8434R")
        assertThat(entity.captured.headers.getFirst("appkey")).isEqualTo("paper-key")
        assertThat(snapshot.currencyBalances).hasSize(1)
        val krw = snapshot.currencyBalances.single()
        assertThat(krw.currency).isEqualTo("KRW")
        assertThat(krw.cashBalance).isEqualByComparingTo("1000000.01")
        assertThat(krw.cashBuyingPower).isNull()
        assertThat(krw.marketValue).isEqualByComparingTo("248500")
        assertThat(krw.unrealizedPnl).isEqualByComparingTo("3500")
        assertThat(krw.returnRate).isEqualByComparingTo("1.43")
        assertThat(snapshot.positions).hasSize(1)
        assertThat(snapshot.positions[0].currency).isEqualTo("KRW")
        assertThat(snapshot.positions[0].ticker).isEqualTo("005930")
        assertThat(snapshot.positions[0].quantity).isEqualByComparingTo("3.5000")
        assertThat(snapshot.asOf).isEqualTo(LocalDateTime.of(2026, 11, 16, 10, 0))
    }

    @Test
    fun `KIS_LIVE 는 live endpoint 와 TTTC8434R 로 호출한다`() {
        val entity = slot<HttpEntity<*>>()
        every { tokenManager.getToken("live") } returns "live-token"
        every {
            restTemplate.exchange(
                match<String> { it.startsWith("https://live.example/") },
                HttpMethod.GET,
                capture(entity),
                Map::class.java,
            )
        } returns ResponseEntity.ok(
            mapOf(
                "rt_cd" to "0",
                "output1" to emptyList<Map<String, String>>(),
                "output2" to listOf(mapOf("dnca_tot_amt" to "0", "scts_evlu_amt" to "0", "evlu_pfls_smtl_amt" to "0")),
            ),
        )

        adapter.fetchBalance(liveRef)

        assertThat(entity.captured.headers.getFirst("tr_id")).isEqualTo("TTTC8434R")
        assertThat(entity.captured.headers.getFirst("appkey")).isEqualTo("live-key")
    }

    @Test
    fun `KIS 가 아닌 모드는 내부 오류이고 토큰도 KIS 호출도 0회이다`() {
        listOf(TradingMode.LOCAL, TradingMode.UPBIT_LIVE, TradingMode.TOSS_LIVE).forEach { mode ->
            val ex = assertThrows(InternalErrorException::class.java) {
                adapter.fetchBalance(BrokerAccountRef(1L, mode, "12345678-01"))
            }

            assertThat(ex.message).contains(mode.name).doesNotContain("12345678")
        }
        verify(exactly = 0) { tokenManager.getToken(any()) }
        verify(exactly = 0) { restTemplate.exchange(any<String>(), any<HttpMethod>(), any(), Map::class.java) }
    }

    // devkit:test-change D28 DEC-05 A — 잔고 경로 오류 변환을 범주 기반 예외(J 503, F-4 409)로 맞춘 기준, 옛 401·403·502·504 단언을 대체
    @Test
    fun `KIS 인증 오류는 KIS_AUTH_FAILED 로 변환하고 KIS 원문은 cause 에만 둔다`() {
        stubRtCd("1", "token expired")

        val ex = assertThrows(KisAuthFailedException::class.java) { adapter.fetchBalance(paperRef) }

        assertThat(ex.errorCode).isEqualTo("KIS_AUTH_FAILED")
        assertThat(ex.message).doesNotContain("token expired").doesNotContain("12345678")
        assertThat(ex.cause).isInstanceOf(KisApiException::class.java)
        assertThat(ex.cause?.message).contains("token expired")
    }

    @Test
    fun `KIS 권한 오류는 KIS_PERMISSION_DENIED 로 변환한다`() {
        stubRtCd("7", "권한 없음")

        val ex = assertThrows(KisPermissionDeniedException::class.java) { adapter.fetchBalance(paperRef) }

        assertThat(ex.errorCode).isEqualTo("KIS_PERMISSION_DENIED")
        assertThat(ex.message).doesNotContain("권한 없음").doesNotContain("12345678")
    }

    // devkit:test-change S9-02 step 11 — 한도 판정을 msg_cd EGW00201·«초당 거래건수» 로 좁힌 기준
    // 실재 확인 안 된 rt_cd 9 + 영문 문구 예시를 KIS 한도 응답 모양으로 대체
    @Test
    fun `rt_cd 1 이라도 msg_cd EGW00201 이면 인증이 아니라 KIS_RATE_LIMITED 로 변환한다`() {
        stubRtCd("1", "초당 거래건수를 초과하였습니다.", msgCd = "EGW00201")

        val ex = assertThrows(KisRateLimitedException::class.java) { adapter.fetchBalance(paperRef) }

        assertThat(ex.errorCode).isEqualTo("KIS_RATE_LIMITED")
        assertThat(ex.message).doesNotContain("초당 거래건수")
    }

    @Test
    fun `rt_cd 1 이고 msg_cd 가 없어도 초당 거래건수 문구면 KIS_RATE_LIMITED 로 변환한다`() {
        stubRtCd("1", "초당 거래건수를 초과하였습니다.")

        val ex = assertThrows(KisRateLimitedException::class.java) { adapter.fetchBalance(paperRef) }

        assertThat(ex.errorCode).isEqualTo("KIS_RATE_LIMITED")
    }

    @Test
    fun `초과 문구만으로는 한도로 보지 않는다`() {
        stubRtCd("9", "조회 가능 건수를 초과하였습니다.")

        val ex = assertThrows(KisUnavailableException::class.java) { adapter.fetchBalance(paperRef) }

        assertThat(ex.errorCode).isEqualTo("KIS_UNAVAILABLE")
    }

    @Test
    fun `그 밖의 KIS 오류는 KIS_UNAVAILABLE 로 변환한다`() {
        stubRtCd("9", "temporary unavailable")

        val ex = assertThrows(KisUnavailableException::class.java) { adapter.fetchBalance(paperRef) }

        assertThat(ex.errorCode).isEqualTo("KIS_UNAVAILABLE")
        assertThat(ex.message).doesNotContain("temporary unavailable")
    }

    // devkit:test-change S9-01 step 11 — 타임아웃을 원인 체인(SocketTimeoutException·HttpTimeoutException)으로만 판정
    // 원인 없는 예외를 RestTemplate 이 실제로 던지는 모양(원인 포함)으로 대체
    @Test
    fun `KIS 접속 시간 초과는 KIS_TIMEOUT 로 변환한다`() {
        stubTransportFailure(ResourceAccessException("I/O error", SocketTimeoutException("Read timed out")))

        val ex = assertThrows(KisTimeoutException::class.java) { adapter.fetchBalance(paperRef) }

        assertThat(ex.errorCode).isEqualTo("KIS_TIMEOUT")
        assertThat(ex.cause).isInstanceOf(ResourceAccessException::class.java)
    }

    @Test
    fun `KIS 연결 거부는 타임아웃이 아니라 KIS_UNAVAILABLE 로 변환한다`() {
        stubTransportFailure(ResourceAccessException("I/O error", ConnectException("Connection refused")))

        val ex = assertThrows(KisUnavailableException::class.java) { adapter.fetchBalance(paperRef) }

        assertThat(ex.errorCode).isEqualTo("KIS_UNAVAILABLE")
        assertThat(ex.cause).isInstanceOf(ResourceAccessException::class.java)
    }

    @Test
    fun `KIS 주소 해석 실패는 KIS_UNAVAILABLE 로 변환한다`() {
        stubTransportFailure(ResourceAccessException("I/O error", UnknownHostException("paper.example")))

        val ex = assertThrows(KisUnavailableException::class.java) { adapter.fetchBalance(paperRef) }

        assertThat(ex.errorCode).isEqualTo("KIS_UNAVAILABLE")
    }

    @Test
    fun `KIS 호출의 그 밖의 전송 오류는 KIS_UNAVAILABLE 로 변환한다`() {
        stubTransportFailure(RestClientException("502 Bad Gateway"))

        val ex = assertThrows(KisUnavailableException::class.java) { adapter.fetchBalance(paperRef) }

        assertThat(ex.errorCode).isEqualTo("KIS_UNAVAILABLE")
        assertThat(ex.cause).isInstanceOf(RestClientException::class.java)
    }

    // 계좌 연결 검사는 서비스가 먼저 하지만 형식이 틀린 ref 가 오면 어댑터도 KIS 를 부르지 않음
    @Test
    fun `ref 의 externalAccountId 형식이 틀리면 KIS 호출 없이 KIS_ACCOUNT_NOT_LINKED 로 변환하고 값을 싣지 않는다`() {
        val ex = assertThrows(KisAccountNotLinkedException::class.java) {
            adapter.fetchBalance(BrokerAccountRef(1L, TradingMode.KIS_PAPER, "99998888"))
        }

        assertThat(ex.errorCode).isEqualTo("KIS_ACCOUNT_NOT_LINKED")
        assertThat(ex.message).doesNotContain("99998888")
        verify(exactly = 0) { restTemplate.exchange(any<String>(), any<HttpMethod>(), any(), Map::class.java) }
    }

    private fun stubRtCd(rtCd: String, msg: String, msgCd: String? = null) {
        val body = mapOf("rt_cd" to rtCd, "msg1" to msg) + (msgCd?.let { mapOf("msg_cd" to it) } ?: emptyMap())
        every { tokenManager.getToken("paper") } returns "paper-token"
        every { restTemplate.exchange(any<String>(), HttpMethod.GET, any(), Map::class.java) } returns
            ResponseEntity.ok(body)
    }

    // devkit:test-change C-02 step 11 리뷰 — KIS 오류 본문이 HTTP 4xx·5xx 로 와도 본문으로 분류
    @Test
    fun `HTTP 500 이라도 본문이 EGW00201 이면 KIS_RATE_LIMITED 이다`() {
        stubTransportFailure(httpError(500, kisBody("1", "EGW00201", "초당 거래건수를 초과하였습니다.")))

        val ex = assertThrows(KisRateLimitedException::class.java) { adapter.fetchBalance(paperRef) }

        assertThat(ex.errorCode).isEqualTo("KIS_RATE_LIMITED")
    }

    @Test
    fun `HTTP 500 이라도 본문이 토큰 만료면 KIS_AUTH_FAILED 이다`() {
        stubTransportFailure(httpError(500, kisBody("1", "EGW00123", "기간이 만료된 token 입니다.")))

        assertThrows(KisAuthFailedException::class.java) { adapter.fetchBalance(paperRef) }
    }

    @Test
    fun `HTTP 500 빈 본문은 KIS_UNAVAILABLE 이다`() {
        stubTransportFailure(httpError(500, ""))

        assertThrows(KisUnavailableException::class.java) { adapter.fetchBalance(paperRef) }
    }

    // step 14 QA Q-01 — 5xx 본문은 한도·인증만 확정, rt_cd 1 서버 에러를 인증 실패로 보지 않음
    @Test
    fun `HTTP 500 본문이 rt_cd 1 서버 에러면 인증이 아니라 KIS_UNAVAILABLE 이다`() {
        stubTransportFailure(httpError(500, kisBody("1", "EGW00002", "서버 에러가 발생 했습니다.")))

        assertThrows(KisUnavailableException::class.java) { adapter.fetchBalance(paperRef) }
    }

    @Test
    fun `HTTP 500 본문이 권한 문구여도 KIS_UNAVAILABLE 이다`() {
        stubTransportFailure(httpError(500, kisBody("7", "EGW00999", "권한이 없습니다.")))

        assertThrows(KisUnavailableException::class.java) { adapter.fetchBalance(paperRef) }
    }

    @Test
    fun `HTTP 4xx 본문 rt_cd 1 은 그대로 KIS_AUTH_FAILED 이다`() {
        stubTransportFailure(httpError(403, kisBody("1", "EGW00002", "서버 에러가 발생 했습니다.")))

        assertThrows(KisAuthFailedException::class.java) { adapter.fetchBalance(paperRef) }
    }

    private fun stubTransportFailure(failure: RestClientException) {
        every { tokenManager.getToken("paper") } returns "paper-token"
        every { restTemplate.exchange(any<String>(), HttpMethod.GET, any(), Map::class.java) } throws failure
    }
}
