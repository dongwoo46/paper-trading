package com.papertrading.api.characterization.kis

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.infrastructure.kis.KisTokenManager
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestConstructor
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.client.RestTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * 특성화 시험(step 8, D21 a): GET /api/kis/account/balance 를 서비스 mock 없이 끝까지 호출한다.
 * KIS 응답은 MockRestServiceServer fixture, 토큰 관리자만 mock. 입력을 고정하고 밖으로 보이는 결과(응답 JSON 통째 + KIS 호출 URL·헤더)를 단언한다.
 * 규칙: 이후 step 9~14 에서 이 시험은 고치지 않고 통과시킨다 (고쳐야 하면 멈추고 사람 결정).
 */
@SpringBootTest(
    properties = [
        "trading.kis.paper-app-key=paper-app-key",
        "trading.kis.paper-app-secret=paper-app-secret",
        "trading.kis.live-app-key=live-app-key",
        "trading.kis.live-app-secret=live-app-secret",
    ],
)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class KisAccountBalanceBoundaryCharacterizationTest(
    private val mockMvc: MockMvc,
    private val restTemplate: RestTemplate,
    private val accountRepository: AccountRepository,
    private val positionRepository: PositionRepository,
) {
    @MockitoBean
    lateinit var kisTokenManager: KisTokenManager

    private lateinit var kisServer: MockRestServiceServer
    private lateinit var originalRequestFactory: ClientHttpRequestFactory

    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @Container
        @ServiceConnection(name = "redis")
        val redis = GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379)

        private const val PAPER_BALANCE_URL =
            "https://openapivts.koreainvestment.com:29443/uapi/domestic-stock/v1/trading/inquire-balance" +
                "?CANO=12345678&ACNT_PRDT_CD=01&AFHR_FLPR_YN=N&OFL_YN=&INQR_DVSN=01&UNPR_DVSN=01" +
                "&FUND_STTL_ICLD_YN=N&FNCG_AMT_AUTO_RDPT_YN=N&PRCS_DVSN=00&CTX_AREA_FK100=&CTX_AREA_NK100="
        private const val LIVE_BALANCE_URL =
            "https://openapi.koreainvestment.com:9443/uapi/domestic-stock/v1/trading/inquire-balance" +
                "?CANO=87654321&ACNT_PRDT_CD=01&AFHR_FLPR_YN=N&OFL_YN=&INQR_DVSN=01&UNPR_DVSN=01" +
                "&FUND_STTL_ICLD_YN=N&FNCG_AMT_AUTO_RDPT_YN=N&PRCS_DVSN=00&CTX_AREA_FK100=&CTX_AREA_NK100="
        private const val AS_OF_PLACEHOLDER = "<AS_OF>"
    }

    @BeforeEach
    fun bindKisServer() {
        originalRequestFactory = restTemplate.requestFactory
        kisServer = MockRestServiceServer.bindTo(restTemplate).build()
        given(kisTokenManager.getToken("paper")).willReturn("paper-token")
        given(kisTokenManager.getToken("live")).willReturn("live-token")
    }

    @AfterEach
    fun restoreRequestFactory() {
        restTemplate.requestFactory = originalRequestFactory
    }

    @Test
    fun `PAPER 조회는 모의 주소와 VTTC8434R 로 KIS 를 부르고 응답 JSON 을 통째로 내보낸다`() {
        val accountId = saveAccount("balance-paper", TradingMode.KIS_PAPER, "12345678-01")
        saveLocalHolding(accountId, "005930", "10", "70000")
        saveLocalHolding(accountId, "035720", "5", "50000")
        saveLocalHolding(accountId, "000660", "2", "120000")
        kisServer.expect(requestTo(PAPER_BALANCE_URL))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("authorization", "Bearer paper-token"))
            .andExpect(header("appkey", "paper-app-key"))
            .andExpect(header("appsecret", "paper-app-secret"))
            .andExpect(header("tr_id", "VTTC8434R"))
            .andExpect(header("custtype", "P"))
            .andExpect(header("Content-Type", "application/json"))
            .andRespond(withSuccess(THREE_POSITION_RESPONSE, MediaType.APPLICATION_JSON))

        val body = getBalance(accountId, "PAPER")

        kisServer.verify()
        assertThat(withoutAsOf(body)).isEqualTo(
            """{"accountId":$accountId,"source":"KIS","mode":"PAPER","asOf":"$AS_OF_PLACEHOLDER",""" +
                """"cashBalance":9300000.50,"marketValue":1252000,"unrealizedPnl":2000,"returnRate":0.16,""" +
                """"positions":[""" +
                """{"ticker":"005930","quantity":10,"avgPrice":70000.0000,"currentPrice":71000,""" +
                """"marketValue":710000,"unrealizedPnl":10000,"returnRate":1.42857143},""" +
                """{"ticker":"035720","quantity":3,"avgPrice":50000,"currentPrice":49000,"marketValue":147000,""" +
                """"unrealizedPnl":-3000,"returnRate":-2.00},""" +
                """{"ticker":"373220","quantity":1,"avgPrice":400000,"currentPrice":395000,"marketValue":395000,""" +
                """"unrealizedPnl":-5000,"returnRate":-1.25}],""" +
                """"reconciliation":{"missingInLocal":["373220"],"missingInKis":["000660"],""" +
                """"quantityMismatch":["035720"]}}""",
        )
        assertAsOfIsRecentUtc(body)
        verify(kisTokenManager).getToken("paper")
        verifyNoMoreInteractions(kisTokenManager)
    }

    @Test
    fun `LIVE 조회는 실전 주소와 TTTC8434R 로 KIS 를 부른다`() {
        val accountId = saveAccount("balance-live", TradingMode.KIS_LIVE, "87654321-01")
        saveLocalHolding(accountId, "005930", "4", "70000")
        kisServer.expect(requestTo(LIVE_BALANCE_URL))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("authorization", "Bearer live-token"))
            .andExpect(header("appkey", "live-app-key"))
            .andExpect(header("appsecret", "live-app-secret"))
            .andExpect(header("tr_id", "TTTC8434R"))
            .andExpect(header("custtype", "P"))
            .andExpect(header("Content-Type", "application/json"))
            .andRespond(withSuccess(ONE_POSITION_RESPONSE, MediaType.APPLICATION_JSON))

        val body = getBalance(accountId, "LIVE")

        kisServer.verify()
        assertThat(withoutAsOf(body)).isEqualTo(
            """{"accountId":$accountId,"source":"KIS","mode":"LIVE","asOf":"$AS_OF_PLACEHOLDER",""" +
                """"cashBalance":500000,"marketValue":284000,"unrealizedPnl":4000,"returnRate":1.43,"positions":[""" +
                """{"ticker":"005930","quantity":4,"avgPrice":70000,"currentPrice":71000,"marketValue":284000,""" +
                """"unrealizedPnl":4000,"returnRate":1.43}],""" +
                """"reconciliation":{"missingInLocal":[],"missingInKis":[],"quantityMismatch":[]}}""",
        )
        assertAsOfIsRecentUtc(body)
        verify(kisTokenManager).getToken("live")
        verifyNoMoreInteractions(kisTokenManager)
    }

    @Test
    fun `trId 와 주소는 계좌 모드가 아니라 mode 파라미터가 정한다`() {
        val accountId = saveAccount("balance-paper-account-live-param", TradingMode.KIS_PAPER, "87654321-01")
        kisServer.expect(requestTo(LIVE_BALANCE_URL))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("authorization", "Bearer live-token"))
            .andExpect(header("appkey", "live-app-key"))
            .andExpect(header("tr_id", "TTTC8434R"))
            .andRespond(withSuccess(EMPTY_RESPONSE, MediaType.APPLICATION_JSON))

        val body = getBalance(accountId, "LIVE")

        kisServer.verify()
        assertThat(withoutAsOf(body)).isEqualTo(
            """{"accountId":$accountId,"source":"KIS","mode":"LIVE","asOf":"$AS_OF_PLACEHOLDER",""" +
                """"cashBalance":0,"marketValue":0,"unrealizedPnl":0,"returnRate":0,"positions":[],""" +
                """"reconciliation":{"missingInLocal":[],"missingInKis":[],"quantityMismatch":[]}}""",
        )
    }

    private fun saveAccount(name: String, mode: TradingMode, externalAccountId: String): Long {
        val account = accountRepository.save(
            Account.create(
                accountName = name,
                accountType = AccountType.STOCK,
                tradingMode = mode,
                initialDeposit = BigDecimal("10000000"),
                externalAccountId = externalAccountId,
            ),
        )
        return requireNotNull(account.id) { "saved account.id is null" }
    }

    private fun saveLocalHolding(accountId: Long, ticker: String, quantity: String, avgPrice: String) {
        val account = accountRepository.getReferenceById(accountId)
        positionRepository.save(
            Position.createWithHolding(account, ticker, MarketType.KOSPI, BigDecimal(quantity), BigDecimal(avgPrice)),
        )
    }

    private fun getBalance(accountId: Long, mode: String): String {
        val result = mockMvc.get("/api/kis/account/balance?accountId=$accountId&mode=$mode")
            .andExpect { status { isOk() } }
            .andReturn()
        return result.response.contentAsString
    }

    private fun withoutAsOf(body: String): String {
        val asOfField = Regex(""""asOf":"[^"]+"""")
        return body.replace(asOfField, """"asOf":"$AS_OF_PLACEHOLDER"""")
    }

    // asOf 는 호출 시각이라 값은 못 박지 못하고 UTC 오프셋 ISO 문자열이라는 모양과 «방금» 만 본다
    private fun assertAsOfIsRecentUtc(body: String) {
        val raw = requireNotNull(Regex(""""asOf":"([^"]+)"""").find(body)) { "asOf missing: $body" }.groupValues[1]
        val asOf = OffsetDateTime.parse(raw)
        assertThat(asOf.offset).isEqualTo(ZoneOffset.UTC)
        assertThat(Duration.between(asOf, OffsetDateTime.now(ZoneOffset.UTC)).abs()).isLessThan(Duration.ofMinutes(1))
    }
}

private const val THREE_POSITION_RESPONSE = """{
  "rt_cd": "0", "msg_cd": "KIOK0510", "msg1": "조회가 완료되었습니다",
  "output1": [
    {"pdno": "005930", "prdt_name": "삼성전자", "hldg_qty": "10", "pchs_avg_pric": "70000.0000", "prpr": "71000",
     "evlu_amt": "710000", "evlu_pfls_amt": "10000", "evlu_erng_rt": "1.42857143"},
    {"pdno": " ", "hldg_qty": "0"},
    {"pdno": "035720", "prdt_name": "카카오", "hldg_qty": "3", "pchs_avg_pric": "50000", "prpr": "49000",
     "evlu_amt": "147000", "evlu_pfls_amt": "-3000"},
    {"pdno": "373220", "prdt_name": "LG에너지솔루션", "hldg_qty": "1", "pchs_avg_pric": "400000", "prpr": "395000",
     "evlu_amt": "395000", "evlu_pfls_amt": "-5000", "evlu_erng_rt": "-1.25"}
  ],
  "output2": [{"dnca_tot_amt": "9,300,000.50", "scts_evlu_amt": "1252000", "evlu_pfls_smtl_amt": "2000"}]
}"""

private const val ONE_POSITION_RESPONSE = """{
  "rt_cd": "0", "msg_cd": "KIOK0510", "msg1": "조회가 완료되었습니다",
  "output1": [
    {"pdno": "005930", "hldg_qty": "4", "pchs_avg_pric": "70000", "prpr": "71000",
     "evlu_amt": "284000", "evlu_pfls_amt": "4000", "evlu_erng_rt": "1.43"}
  ],
  "output2": [
    {"dnca_tot_amt": "500000", "scts_evlu_amt": "284000", "evlu_pfls_smtl_amt": "4000", "evlu_erng_rt": "1.43"}
  ]
}"""

private const val EMPTY_RESPONSE = """{
  "rt_cd": "0", "msg1": "조회가 완료되었습니다",
  "output1": [],
  "output2": [{"dnca_tot_amt": "0", "scts_evlu_amt": "0", "evlu_pfls_smtl_amt": "0"}]
}"""
