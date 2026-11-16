package com.papertrading.api.infrastructure.kis

import com.papertrading.api.support.withId
import com.papertrading.api.common.exception.KisAccountNotLinkedException
import com.papertrading.api.common.exception.KisAuthFailedException
import com.papertrading.api.common.exception.KisPermissionDeniedException
import com.papertrading.api.common.exception.KisRateLimitedException
import com.papertrading.api.common.exception.KisTimeoutException
import com.papertrading.api.common.exception.KisUnavailableException
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.infrastructure.persistence.AccountRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.util.Optional

class KisAccountBalanceAdapterTest {

    private val accountRepository = mockk<AccountRepository>()
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
    private val adapter = KisAccountBalanceAdapter(accountRepository, properties, tokenManager, restTemplate)

    @Test
    fun `PAPER trId면 paper endpoint를 호출하고 BigDecimal snapshot으로 매핑한다`() {
        every { accountRepository.findById(1L) } returns Optional.of(sampleAccount())
        every { tokenManager.getToken("paper") } returns "paper-token"
        every {
            restTemplate.exchange(
                match<String> { it.startsWith("https://paper.example/") },
                HttpMethod.GET,
                any(),
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
                    )
                ),
                "output2" to listOf(
                    mapOf(
                        "dnca_tot_amt" to "1000000.01",
                        "scts_evlu_amt" to "248500",
                        "evlu_pfls_smtl_amt" to "3500",
                        "evlu_erng_rt" to "1.43",
                    )
                ),
            )
        )

        val snapshot = adapter.fetchBalance(1L, "VTTC8434R")

        assertThat(snapshot.cashBalance).isEqualByComparingTo("1000000.01")
        assertThat(snapshot.marketValue).isEqualByComparingTo("248500")
        assertThat(snapshot.unrealizedPnl).isEqualByComparingTo("3500")
        assertThat(snapshot.returnRate).isEqualByComparingTo("1.43")
        assertThat(snapshot.positions).hasSize(1)
        assertThat(snapshot.positions[0].quantity).isEqualByComparingTo("3.5000")
    }

    @Test
    fun `LIVE trId면 live endpoint를 호출한다`() {
        every { accountRepository.findById(1L) } returns Optional.of(sampleAccount())
        every { tokenManager.getToken("live") } returns "live-token"
        every {
            restTemplate.exchange(
                match<String> { it.startsWith("https://live.example/") },
                HttpMethod.GET,
                any(),
                Map::class.java,
            )
        } returns ResponseEntity.ok(
            mapOf(
                "rt_cd" to "0",
                "output1" to emptyList<Map<String, String>>(),
                "output2" to listOf(mapOf("dnca_tot_amt" to "0", "scts_evlu_amt" to "0", "evlu_pfls_smtl_amt" to "0")),
            )
        )

        adapter.fetchBalance(1L, "TTTC8434R")
    }

    // devkit:test-change D28 DEC-05 A — 잔고 경로 오류 변환을 범주 기반 예외(J 503, F-4 409)로 맞춘 기준, 옛 401·403·502·504 단언을 대체
    @Test
    fun `KIS 인증 오류는 KIS_AUTH_FAILED 로 변환하고 KIS 원문은 cause 에만 둔다`() {
        stubRtCd("1", "token expired")

        val ex = assertThrows(KisAuthFailedException::class.java) { adapter.fetchBalance(1L, "VTTC8434R") }

        assertThat(ex.errorCode).isEqualTo("KIS_AUTH_FAILED")
        assertThat(ex.message).doesNotContain("token expired").doesNotContain("12345678")
        assertThat(ex.cause).isInstanceOf(KisApiException::class.java)
        assertThat(ex.cause?.message).contains("token expired")
    }

    @Test
    fun `KIS 권한 오류는 KIS_PERMISSION_DENIED 로 변환한다`() {
        stubRtCd("7", "권한 없음")

        val ex = assertThrows(KisPermissionDeniedException::class.java) { adapter.fetchBalance(1L, "VTTC8434R") }

        assertThat(ex.errorCode).isEqualTo("KIS_PERMISSION_DENIED")
        assertThat(ex.message).doesNotContain("권한 없음").doesNotContain("12345678")
    }

    @Test
    fun `호출 한도 문구의 KIS 오류는 KIS_RATE_LIMITED 로 변환한다`() {
        stubRtCd("9", "rate limit exceeded")

        val ex = assertThrows(KisRateLimitedException::class.java) { adapter.fetchBalance(1L, "VTTC8434R") }

        assertThat(ex.errorCode).isEqualTo("KIS_RATE_LIMITED")
        assertThat(ex.message).doesNotContain("rate limit exceeded")
    }

    @Test
    fun `그 밖의 KIS 오류는 KIS_UNAVAILABLE 로 변환한다`() {
        stubRtCd("9", "temporary unavailable")

        val ex = assertThrows(KisUnavailableException::class.java) { adapter.fetchBalance(1L, "VTTC8434R") }

        assertThat(ex.errorCode).isEqualTo("KIS_UNAVAILABLE")
        assertThat(ex.message).doesNotContain("temporary unavailable")
    }

    @Test
    fun `KIS 접속 시간 초과는 KIS_TIMEOUT 로 변환한다`() {
        stubTransportFailure(ResourceAccessException("Read timed out"))

        val ex = assertThrows(KisTimeoutException::class.java) { adapter.fetchBalance(1L, "VTTC8434R") }

        assertThat(ex.errorCode).isEqualTo("KIS_TIMEOUT")
        assertThat(ex.cause).isInstanceOf(ResourceAccessException::class.java)
    }

    @Test
    fun `KIS 호출의 그 밖의 전송 오류는 KIS_UNAVAILABLE 로 변환한다`() {
        stubTransportFailure(RestClientException("502 Bad Gateway"))

        val ex = assertThrows(KisUnavailableException::class.java) { adapter.fetchBalance(1L, "VTTC8434R") }

        assertThat(ex.errorCode).isEqualTo("KIS_UNAVAILABLE")
        assertThat(ex.cause).isInstanceOf(RestClientException::class.java)
    }

    @Test
    fun `externalAccountId 가 없으면 KIS 호출 없이 KIS_ACCOUNT_NOT_LINKED 로 변환한다`() {
        every { accountRepository.findById(1L) } returns Optional.of(sampleAccount(externalAccountId = null))

        val ex = assertThrows(KisAccountNotLinkedException::class.java) { adapter.fetchBalance(1L, "VTTC8434R") }

        assertThat(ex.errorCode).isEqualTo("KIS_ACCOUNT_NOT_LINKED")
        verify(exactly = 0) { restTemplate.exchange(any<String>(), any<HttpMethod>(), any(), Map::class.java) }
    }

    @Test
    fun `externalAccountId 형식이 틀리면 KIS 호출 없이 KIS_ACCOUNT_NOT_LINKED 로 변환하고 값을 싣지 않는다`() {
        every { accountRepository.findById(1L) } returns Optional.of(sampleAccount(externalAccountId = "99998888"))

        val ex = assertThrows(KisAccountNotLinkedException::class.java) { adapter.fetchBalance(1L, "VTTC8434R") }

        assertThat(ex.errorCode).isEqualTo("KIS_ACCOUNT_NOT_LINKED")
        assertThat(ex.message).doesNotContain("99998888")
        verify(exactly = 0) { restTemplate.exchange(any<String>(), any<HttpMethod>(), any(), Map::class.java) }
    }

    private fun stubRtCd(rtCd: String, msg: String) {
        every { accountRepository.findById(1L) } returns Optional.of(sampleAccount())
        every { tokenManager.getToken("paper") } returns "paper-token"
        every { restTemplate.exchange(any<String>(), HttpMethod.GET, any(), Map::class.java) } returns
            ResponseEntity.ok(mapOf("rt_cd" to rtCd, "msg1" to msg))
    }

    private fun stubTransportFailure(failure: RestClientException) {
        every { accountRepository.findById(1L) } returns Optional.of(sampleAccount())
        every { tokenManager.getToken("paper") } returns "paper-token"
        every { restTemplate.exchange(any<String>(), HttpMethod.GET, any(), Map::class.java) } throws failure
    }

    private fun sampleAccount(externalAccountId: String? = "12345678-01"): Account = Account.create(
        accountName = "kis",
        accountType = AccountType.STOCK,
        tradingMode = TradingMode.KIS_PAPER,
        initialDeposit = BigDecimal("1000000"),
        externalAccountId = externalAccountId,
    ).withId(1L)
}
