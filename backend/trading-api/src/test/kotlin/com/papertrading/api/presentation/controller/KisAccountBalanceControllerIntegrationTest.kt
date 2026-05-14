package com.papertrading.api.presentation.controller

import com.papertrading.api.application.account.KisAccountQueryService
import com.papertrading.api.application.account.result.KisAccountBalanceResult
import com.papertrading.api.application.account.result.KisBalancePositionResult
import com.papertrading.api.application.account.result.KisReconciliationResult
import com.papertrading.api.common.exception.KisAuthorizationException
import com.papertrading.api.common.exception.KisForbiddenException
import com.papertrading.api.common.exception.KisRemoteCallException
import com.papertrading.api.common.exception.KisTimeoutException
import com.papertrading.api.domain.enums.KisAccountMode
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

    @Test
    fun `오류 매핑 400 401 403 502 504`() {
        given(kisAccountQueryService.getBalance(0L, KisAccountMode.LIVE)).willThrow(IllegalArgumentException("bad"))
        given(kisAccountQueryService.getBalance(1L, KisAccountMode.PAPER)).willThrow(KisAuthorizationException("unauthorized"))
        given(kisAccountQueryService.getBalance(2L, KisAccountMode.PAPER)).willThrow(KisForbiddenException("forbidden"))
        given(kisAccountQueryService.getBalance(3L, KisAccountMode.PAPER)).willThrow(KisRemoteCallException("remote"))
        given(kisAccountQueryService.getBalance(4L, KisAccountMode.PAPER)).willThrow(KisTimeoutException("timeout"))

        mockMvc.get("/api/kis/account/balance?accountId=0&mode=LIVE").andExpect { status { isBadRequest() } }
        mockMvc.get("/api/kis/account/balance?accountId=1&mode=PAPER").andExpect {
            status { isUnauthorized() }
            jsonPath("$.message") { value("unauthorized") }
        }
        mockMvc.get("/api/kis/account/balance?accountId=2&mode=PAPER").andExpect {
            status { isForbidden() }
            jsonPath("$.message") { value("forbidden") }
        }
        mockMvc.get("/api/kis/account/balance?accountId=3&mode=PAPER").andExpect {
            status { isBadGateway() }
            jsonPath("$.message") { value("remote") }
        }
        mockMvc.get("/api/kis/account/balance?accountId=4&mode=PAPER").andExpect {
            status { isGatewayTimeout() }
            jsonPath("$.message") { value("timeout") }
        }
    }
}
