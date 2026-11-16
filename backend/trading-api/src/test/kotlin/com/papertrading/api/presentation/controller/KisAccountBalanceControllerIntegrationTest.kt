package com.papertrading.api.presentation.controller

import com.papertrading.api.application.account.KisAccountQueryService
import com.papertrading.api.application.account.result.KisAccountBalanceResult
import com.papertrading.api.application.account.result.KisBalancePositionResult
import com.papertrading.api.application.account.result.KisReconciliationResult
import com.papertrading.api.common.exception.KisAccountNotLinkedException
import com.papertrading.api.common.exception.KisAuthFailedException
import com.papertrading.api.common.exception.KisPermissionDeniedException
import com.papertrading.api.common.exception.KisRateLimitedException
import com.papertrading.api.common.exception.KisTimeoutException
import com.papertrading.api.common.exception.KisUnavailableException
import com.papertrading.api.domain.enums.KisAccountMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.time.OffsetDateTime

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class KisAccountBalanceControllerIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc

    @MockitoBean lateinit var kisAccountQueryService: KisAccountQueryService

    companion object {
        private const val KIS_RAW_MESSAGE = "KIS-원문-RAW-에러"
        private const val ACCOUNT_NUMBER = "12345678-01"

        @Container @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @Container @ServiceConnection(name = "redis")
        val redis = GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379)
    }

    @Test
    fun `정상 조회시 200 응답을 반환한다`() {
        given(kisAccountQueryService.getBalance(1L, KisAccountMode.LIVE)).willReturn(
            KisAccountBalanceResult(
                accountId = 1L,
                mode = KisAccountMode.LIVE,
                asOf = OffsetDateTime.parse("2026-05-02T10:00:00+09:00"),
                cashBalance = BigDecimal("1000"),
                marketValue = BigDecimal("2000"),
                unrealizedPnl = BigDecimal("10"),
                returnRate = BigDecimal("1.2"),
                positions = listOf(
                    KisBalancePositionResult(
                        ticker = "005930",
                        quantity = BigDecimal("1"),
                        avgPrice = BigDecimal("70000"),
                        currentPrice = BigDecimal("71000"),
                        marketValue = BigDecimal("71000"),
                        unrealizedPnl = BigDecimal("1000"),
                        returnRate = BigDecimal("1.43")
                    )
                ),
                reconciliation = KisReconciliationResult(emptyList(), emptyList(), emptyList())
            )
        )

        mockMvc.get("/api/kis/account/balance?accountId=1&mode=LIVE")
            .andExpect {
                status { isOk() }
                jsonPath("$.accountId") { value(1) }
                jsonPath("$.source") { value("KIS") }
                jsonPath("$.mode") { value("LIVE") }
                jsonPath("$.cashBalance") { value(1000) }
                jsonPath("$.returnRate") { value(1.2) }
                jsonPath("$.positions[0].quantity") { value(1) }
            }
    }

    // devkit:test-change D28 DEC-05 A — 잔고 경로 오류를 범주 기반(J 503, F-4 409)과 code 별 고정 문구로 맞춘 기준, 옛 401·403·502·504 단언 대체
    @Test
    fun `오류 매핑 400 과 KIS 실패 503 와 미연결 409`() {
        given(kisAccountQueryService.getBalance(0L, KisAccountMode.LIVE)).willThrow(IllegalArgumentException("bad"))
        givenPaperFailure(1L, KisAuthFailedException(kisCause()))
        givenPaperFailure(2L, KisPermissionDeniedException(kisCause()))
        givenPaperFailure(3L, KisUnavailableException(kisCause()))
        givenPaperFailure(4L, KisTimeoutException(kisCause()))
        givenPaperFailure(5L, KisRateLimitedException(kisCause()))
        givenPaperFailure(6L, KisAccountNotLinkedException())

        mockMvc.get("/api/kis/account/balance?accountId=0&mode=LIVE").andExpect { status { isBadRequest() } }
        expectError(1L, 503, "KIS_AUTH_FAILED", "KIS 인증에 실패했습니다. KIS 키 설정을 확인해 주세요.")
        expectError(2L, 503, "KIS_PERMISSION_DENIED", "KIS 계좌 조회 권한이 없습니다. KIS 앱 권한 설정을 확인해 주세요.")
        expectError(3L, 503, "KIS_UNAVAILABLE", "KIS를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.")
        expectError(4L, 503, "KIS_TIMEOUT", "KIS 응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요.")
        expectError(5L, 503, "KIS_RATE_LIMITED", "KIS 호출 한도를 넘었습니다. 잠시 후 다시 시도해 주세요.")
        expectError(
            6L,
            409,
            "KIS_ACCOUNT_NOT_LINKED",
            "KIS 계좌와 연결되지 않았습니다. 계좌의 externalAccountId 를 XXXXXXXX-XX 형식으로 등록해 주세요.",
        )
    }

    // cause 에는 KIS 원문과 계좌번호가 있어도 응답 본문 어디에도 나오면 안 된다 (AE8)
    private fun expectError(accountId: Long, status: Int, code: String, message: String) {
        val body = mockMvc.get("/api/kis/account/balance?accountId=$accountId&mode=PAPER")
            .andExpect {
                status { isEqualTo(status) }
                jsonPath("$.status") { value(status) }
                jsonPath("$.code") { value(code) }
                jsonPath("$.message") { value(message) }
            }
            .andReturn().response.contentAsString
        assertThat(body).doesNotContain(KIS_RAW_MESSAGE).doesNotContain(ACCOUNT_NUMBER)
    }

    private fun givenPaperFailure(accountId: Long, failure: RuntimeException) {
        given(kisAccountQueryService.getBalance(accountId, KisAccountMode.PAPER)).willThrow(failure)
    }

    private fun kisCause() = RuntimeException("$KIS_RAW_MESSAGE account=$ACCOUNT_NUMBER")
}
