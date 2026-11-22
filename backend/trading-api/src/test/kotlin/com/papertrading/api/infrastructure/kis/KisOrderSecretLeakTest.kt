package com.papertrading.api.infrastructure.kis

import com.papertrading.api.application.notification.SlackNotificationEventPublisher
import com.papertrading.api.application.order.BrokerOrderSubmitter
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.net.SocketTimeoutException

// 2단계 §10.6 비밀값 로그 누출: KIS 주문 실패 경로 로그에 토큰·app secret·계좌번호 전체 0건 (step 14 QA Q-03)
class KisOrderSecretLeakTest {

    private val properties = KisOrderProperties().apply {
        paperRestBaseUrl = "https://paper.example"
        liveRestBaseUrl = "https://live.example"
        paperAppKey = "paper-key"
        paperAppSecret = "paper-sec-SECRET"
        liveAppKey = "live-key"
        liveAppSecret = "live-sec-SECRET"
    }
    private val tokenManager = mockk<KisTokenManager>()
    private val restTemplate = mockk<RestTemplate>()
    private val submitter = BrokerOrderSubmitter(
        listOf(KisOrderAdapter(KisOrderRestClient(properties, tokenManager, restTemplate))),
        mockk<SlackNotificationEventPublisher>(relaxed = true),
    )

    private fun liveOrder(): Order {
        val account = Account.create(
            "시험 계좌",
            AccountType.STOCK,
            TradingMode.KIS_LIVE,
            BigDecimal("1000000"),
            externalAccountId = ACCOUNT_NUMBER,
        ).withId(1L)
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("failures")
    fun `KIS 주문 실패 로그에 토큰 app secret 계좌번호 전체가 없다`(case: String, stub: (RestTemplate) -> Unit) {
        every { tokenManager.getToken("live") } returns TOKEN
        stub(restTemplate)

        val lines = LogCapture().use { capture ->
            assertThrows<RuntimeException> { submitter.submit(liveOrder()) }
            // mockk 자체 DEBUG 로그는 가짜 객체 호출 인자를 그대로 찍어 대상 밖
            capture.lines().filterNot { it.startsWith("io.mockk") }
        }

        assertThat(lines).describedAs(case).anyMatch { it.contains("BrokerOrderSubmitter") && it.contains("접수 실패") }
        val joined = lines.joinToString("\n")
        listOf(TOKEN, "live-sec-SECRET", "paper-sec-SECRET", ACCOUNT_NUMBER, "12345678").forEach { secret ->
            assertThat(joined).describedAs("$case: $secret").doesNotContain(secret)
        }
    }

    companion object {
        private const val TOKEN = "tok-SECRET"
        private const val ACCOUNT_NUMBER = "12345678-01"

        @JvmStatic
        fun failures(): List<Arguments> = listOf(
            Arguments.of(
                "본문 없는 HTTP 500",
                { rt: RestTemplate ->
                    every { rt.exchange(any<String>(), HttpMethod.POST, any(), Map::class.java) } throws
                        HttpServerErrorException.create(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Internal Server Error",
                            HttpHeaders.EMPTY,
                            ByteArray(0),
                            null,
                        )
                },
            ),
            Arguments.of(
                "읽기 타임아웃",
                { rt: RestTemplate ->
                    every { rt.exchange(any<String>(), HttpMethod.POST, any(), Map::class.java) } throws
                        ResourceAccessException("I/O error", SocketTimeoutException("Read timed out"))
                },
            ),
            Arguments.of(
                "HTTP 200 주문 거부",
                { rt: RestTemplate ->
                    every { rt.exchange(any<String>(), HttpMethod.POST, any(), Map::class.java) } returns
                        ResponseEntity.ok(mapOf("rt_cd" to "1", "msg1" to "주문가능금액을 초과 했습니다"))
                },
            ),
        )
    }
}
