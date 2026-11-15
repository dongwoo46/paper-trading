package com.papertrading.api.presentation.controller

import com.papertrading.api.application.account.BrokerAccountQueryService
import com.papertrading.api.application.account.port.BrokerBalancePosition
import com.papertrading.api.application.account.port.BrokerCurrencyBalance
import com.papertrading.api.application.account.result.BrokerAccountBalanceResult
import com.papertrading.api.common.exception.AccountNotFoundException
import com.papertrading.api.common.exception.BrokerBalanceNotSupportedException
import com.papertrading.api.common.exception.NotBrokerAccountException
import com.papertrading.api.common.exception.TossAccountNotLinkedException
import com.papertrading.api.common.exception.TossIntegrationDisabledException
import com.papertrading.api.common.exception.TossResponseInvalidException
import com.papertrading.api.domain.enums.AccountSource
import com.papertrading.api.domain.enums.TradingMode
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
import java.time.LocalDateTime

// GET /api/v1/accounts/{accountId}/broker-balance: 경로, 상태, code, 금액·수량·가격·비율은 JSON 문자열 (D9, FLAG-10)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class BrokerAccountBalanceControllerIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc

    @MockitoBean lateinit var brokerAccountQueryService: BrokerAccountQueryService

    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @Container
        @ServiceConnection(name = "redis")
        val redis = GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379)

        private const val LONG_DIGITS = "123456789012345.123456"
    }

    private fun result() = BrokerAccountBalanceResult(
        accountId = 7L,
        source = AccountSource.TOSS,
        tradingMode = TradingMode.TOSS_LIVE,
        asOf = LocalDateTime.of(2026, 11, 15, 10, 0),
        currencyBalances = listOf(
            BrokerCurrencyBalance(
                currency = "KRW",
                cashBalance = null,
                cashBuyingPower = BigDecimal("5000000"),
                marketValue = BigDecimal(LONG_DIGITS),
                unrealizedPnl = BigDecimal("700000"),
                returnRate = BigDecimal("12.50"),
            ),
            BrokerCurrencyBalance("USD", null, BigDecimal("3500.5"), null, null, null),
        ),
        positions = listOf(
            BrokerBalancePosition(
                ticker = "005930",
                currency = "KRW",
                quantity = BigDecimal("1E+3"),
                avgPrice = BigDecimal("65000"),
                currentPrice = BigDecimal("72000.50"),
                marketValue = BigDecimal("7200000"),
                unrealizedPnl = BigDecimal("-700000"),
                returnRate = BigDecimal("10.77"),
            ),
        ),
    )

    @Test
    fun `정상 조회는 200 이고 금액 수량 가격 비율이 JSON 문자열로 자릿수와 끝 0 을 그대로 나온다`() {
        given(brokerAccountQueryService.getBalance(7L)).willReturn(result())

        mockMvc.get("/api/v1/accounts/7/broker-balance")
            .andExpect {
                status { isOk() }
                jsonPath("$.accountId") { value(7) }
                jsonPath("$.source") { value("TOSS") }
                jsonPath("$.tradingMode") { value("TOSS_LIVE") }
                jsonPath("$.asOf") { value("2026-11-15T10:00:00") }
                jsonPath("$.currencyBalances.length()") { value(2) }
                jsonPath("$.currencyBalances[0].currency") { value("KRW") }
                jsonPath("$.currencyBalances[0].cashBalance") { value(null) }
                jsonPath("$.currencyBalances[0].cashBuyingPower") { value("5000000") }
                jsonPath("$.currencyBalances[0].marketValue") { value(LONG_DIGITS) }
                jsonPath("$.currencyBalances[0].unrealizedPnl") { value("700000") }
                jsonPath("$.currencyBalances[0].returnRate") { value("12.50") }
                jsonPath("$.currencyBalances[1].currency") { value("USD") }
                jsonPath("$.currencyBalances[1].cashBuyingPower") { value("3500.5") }
                jsonPath("$.currencyBalances[1].marketValue") { value(null) }
                jsonPath("$.currencyBalances[1].unrealizedPnl") { value(null) }
                jsonPath("$.currencyBalances[1].returnRate") { value(null) }
                jsonPath("$.positions[0].ticker") { value("005930") }
                jsonPath("$.positions[0].currency") { value("KRW") }
                jsonPath("$.positions[0].quantity") { value("1000") }
                jsonPath("$.positions[0].avgPrice") { value("65000") }
                jsonPath("$.positions[0].currentPrice") { value("72000.50") }
                jsonPath("$.positions[0].marketValue") { value("7200000") }
                jsonPath("$.positions[0].unrealizedPnl") { value("-700000") }
                jsonPath("$.positions[0].returnRate") { value("10.77") }
            }
    }

    @Test
    fun `금액 칸은 숫자가 아니라 문자열 타입이다`() {
        given(brokerAccountQueryService.getBalance(7L)).willReturn(result())

        mockMvc.get("/api/v1/accounts/7/broker-balance")
            .andExpect {
                jsonPath("$.currencyBalances[0].cashBuyingPower") { isString() }
                jsonPath("$.currencyBalances[0].returnRate") { isString() }
                jsonPath("$.positions[0].quantity") { isString() }
                jsonPath("$.positions[0].returnRate") { isString() }
            }
    }

    @Test
    fun `오류는 범주별 상태와 code 로 매핑된다`() {
        given(brokerAccountQueryService.getBalance(1L)).willThrow(AccountNotFoundException(1L))
        given(brokerAccountQueryService.getBalance(2L)).willThrow(NotBrokerAccountException("LOCAL"))
        given(brokerAccountQueryService.getBalance(3L)).willThrow(BrokerBalanceNotSupportedException("KIS_LIVE"))
        given(brokerAccountQueryService.getBalance(4L)).willThrow(TossAccountNotLinkedException())
        given(brokerAccountQueryService.getBalance(5L)).willThrow(TossResponseInvalidException("필드 경로=items"))
        given(brokerAccountQueryService.getBalance(6L))
            .willThrow(TossIntegrationDisabledException(listOf("TOSS_BASE_URL")))

        expectError(1, 404, "ACCOUNT_NOT_FOUND")
        expectError(2, 404, "NOT_BROKER_ACCOUNT")
        expectError(3, 501, "BROKER_BALANCE_NOT_SUPPORTED")
        expectError(4, 409, "TOSS_ACCOUNT_NOT_LINKED")
        expectError(5, 503, "TOSS_RESPONSE_INVALID")
        expectError(6, 500, "TOSS_INTEGRATION_DISABLED")
    }

    private fun expectError(accountId: Int, status: Int, code: String) {
        mockMvc.get("/api/v1/accounts/$accountId/broker-balance").andExpect {
            status { isEqualTo(status) }
            jsonPath("$.status") { value(status) }
            jsonPath("$.code") { value(code) }
        }
    }
}
