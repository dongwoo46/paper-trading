package com.papertrading.api.infrastructure.kis

import com.papertrading.api.support.withId
import com.papertrading.api.application.account.kis.KisAuthorizationException
import com.papertrading.api.application.account.kis.KisForbiddenException
import com.papertrading.api.application.account.kis.KisRemoteCallException
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.infrastructure.persistence.AccountRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
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

    @Test
    fun `KIS 인증 오류는 authorization exception으로 변환한다`() {
        every { accountRepository.findById(1L) } returns Optional.of(sampleAccount())
        every { tokenManager.getToken("paper") } returns "paper-token"
        every { restTemplate.exchange(any<String>(), HttpMethod.GET, any(), Map::class.java) } returns
            ResponseEntity.ok(mapOf("rt_cd" to "1", "msg1" to "token expired"))

        assertThrows(KisAuthorizationException::class.java) {
            adapter.fetchBalance(1L, "VTTC8434R")
        }
    }

    @Test
    fun `KIS 권한 오류는 forbidden exception으로 변환한다`() {
        every { accountRepository.findById(1L) } returns Optional.of(sampleAccount())
        every { tokenManager.getToken("paper") } returns "paper-token"
        every { restTemplate.exchange(any<String>(), HttpMethod.GET, any(), Map::class.java) } returns
            ResponseEntity.ok(mapOf("rt_cd" to "7", "msg1" to "권한 없음"))

        assertThrows(KisForbiddenException::class.java) {
            adapter.fetchBalance(1L, "VTTC8434R")
        }
    }

    @Test
    fun `기타 KIS 오류는 remote call exception으로 변환한다`() {
        every { accountRepository.findById(1L) } returns Optional.of(sampleAccount())
        every { tokenManager.getToken("paper") } returns "paper-token"
        every { restTemplate.exchange(any<String>(), HttpMethod.GET, any(), Map::class.java) } returns
            ResponseEntity.ok(mapOf("rt_cd" to "9", "msg1" to "temporary unavailable"))

        assertThrows(KisRemoteCallException::class.java) {
            adapter.fetchBalance(1L, "VTTC8434R")
        }
    }

    private fun sampleAccount(): Account = Account.create(
        accountName = "kis",
        accountType = AccountType.STOCK,
        tradingMode = TradingMode.KIS_PAPER,
        initialDeposit = BigDecimal("1000000"),
        externalAccountId = "12345678-01",
    ).withId(1L)
}
