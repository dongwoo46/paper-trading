package com.papertrading.api.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.api.application.order.LocalMatchingEngine
import com.papertrading.api.domain.port.CollectorSubscriptionPort
import com.papertrading.api.domain.port.QuoteSnapshot
import com.papertrading.api.infrastructure.kis.KisOrderRestClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class OrderControllerIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var localMatchingEngine: LocalMatchingEngine
    @Autowired lateinit var redisTemplate: StringRedisTemplate

    // KIS 외부 API 호출 차단 (실제 KIS와 통신하면 안 됨)
    @MockitoBean lateinit var kisOrderRestClient: KisOrderRestClient
    @MockitoBean lateinit var collectorSubscriptionPort: CollectorSubscriptionPort

    companion object {
        @Container @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @Container @ServiceConnection(name = "redis")
        val redis = GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379)
    }

    @BeforeEach
    fun cleanRedis() {
        redisTemplate.keys("quote:*").forEach { redisTemplate.delete(it) }
    }

    // Redis에 시세 직접 저장
    private fun seedQuote(ticker: String, price: String, askp1: String, bidp1: String) {
        val key = "quote:$ticker"
        val ops = redisTemplate.opsForHash<String, String>()
        ops.putAll(key, mapOf(
            "price" to price,
            "askp1" to askp1,
            "bidp1" to bidp1,
            "high" to price,
            "low" to price,
            "volume" to "1000",
            "updatedAt" to Instant.now().toEpochMilli().toString(),
        ))
        redisTemplate.expire(key, Duration.ofSeconds(60))
    }

    private fun createAccountAndDeposit(deposit: Int = 10_000_000): Long {
        val createResult = mockMvc.post("/api/v1/accounts") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf(
                "accountName" to "주문테스트", "accountType" to "STOCK",
                "tradingMode" to "LOCAL", "initialDeposit" to deposit,
            ))
        }.andReturn()
        return objectMapper.readTree(createResult.response.contentAsString)["id"].asLong()
    }

    @Test
    fun `LOCAL 지정가 매수 주문 생성 후 시세 매칭으로 FILLED`() {
        val accountId = createAccountAndDeposit(10_000_000)
        seedQuote("005930", "70000", "70100", "69900")

        // 주문 생성
        val orderResult = mockMvc.post("/api/v1/accounts/$accountId/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf(
                "ticker" to "005930",
                "marketType" to "KOSPI",
                "orderType" to "LIMIT",
                "orderSide" to "BUY",
                "orderCondition" to "DAY",
                "quantity" to 10,
                "limitPrice" to 71000,  // askp1(70100) <= limitPrice(71000) → 체결 조건 충족
                "idempotencyKey" to UUID.randomUUID().toString(),
            ))
        }.andExpect {
            status { isCreated() }
            jsonPath("$.orderStatus") { value("PENDING") }
            jsonPath("$.ticker") { value("005930") }
        }.andReturn()

        val orderId = objectMapper.readTree(orderResult.response.contentAsString)["orderId"].asLong()

        // QuoteEventListener 역할: 시세 이벤트로 매칭 트리거
        val quote = QuoteSnapshot("005930", BigDecimal("70000"), BigDecimal("70100"), BigDecimal("69900"), Instant.now())
        localMatchingEngine.tryMatchPendingOrders("005930", quote)

        // 주문 FILLED 확인
        mockMvc.get("/api/v1/accounts/$accountId/orders/$orderId").andExpect {
            status { isOk() }
            jsonPath("$.orderStatus") { value("FILLED") }
            jsonPath("$.filledQuantity") { value(10) }
        }

        // 포지션 생성 확인
        mockMvc.get("/api/v1/accounts/$accountId/positions/005930").andExpect {
            status { isOk() }
            jsonPath("$.quantity") { value(10) }
            jsonPath("$.ticker") { value("005930") }
        }
    }

    @Test
    fun `LOCAL 시장가 매수 주문 — stale 시세면 400`() {
        val accountId = createAccountAndDeposit()
        // Redis에 시세 없음 (stale)

        mockMvc.post("/api/v1/accounts/$accountId/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf(
                "ticker" to "005930",
                "marketType" to "KOSPI",
                "orderType" to "MARKET",
                "orderSide" to "BUY",
                "orderCondition" to "DAY",
                "quantity" to 1,
                "idempotencyKey" to UUID.randomUUID().toString(),
            ))
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `LOCAL 미보유 종목 매도 시 400`() {
        val accountId = createAccountAndDeposit()

        mockMvc.post("/api/v1/accounts/$accountId/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf(
                "ticker" to "005930",
                "marketType" to "KOSPI",
                "orderType" to "LIMIT",
                "orderSide" to "SELL",
                "orderCondition" to "DAY",
                "quantity" to 5,
                "limitPrice" to 75000,
                "idempotencyKey" to UUID.randomUUID().toString(),
            ))
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `주문 취소 시 예수금 잠금 해제`() {
        val accountId = createAccountAndDeposit(10_000_000)
        seedQuote("005930", "70000", "70100", "69900")

        val orderResult = mockMvc.post("/api/v1/accounts/$accountId/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf(
                "ticker" to "005930",
                "marketType" to "KOSPI",
                "orderType" to "LIMIT",
                "orderSide" to "BUY",
                "orderCondition" to "DAY",
                "quantity" to 10,
                "limitPrice" to 60000,  // askp1(70100) > limitPrice(60000) → 미체결
                "idempotencyKey" to UUID.randomUUID().toString(),
            ))
        }.andExpect {
            status { isCreated() }
            jsonPath("$.orderStatus") { value("PENDING") }
        }.andReturn()

        val orderId = objectMapper.readTree(orderResult.response.contentAsString)["orderId"].asLong()

        // 계좌 잔고 확인 — 잠금 반영
        val beforeCancel = mockMvc.get("/api/v1/accounts/$accountId").andReturn()
        val beforeAvailable = objectMapper.readTree(beforeCancel.response.contentAsString)["availableDeposit"].asLong()

        // 취소
        mockMvc.delete("/api/v1/accounts/$accountId/orders/$orderId").andExpect {
            status { isNoContent() }
        }

        // 잠금 해제 후 잔고 복구 확인
        mockMvc.get("/api/v1/accounts/$accountId").andExpect {
            status { isOk() }
            jsonPath("$.availableDeposit") { value(10_000_000) }
        }
    }

    @Test
    fun `주문 멱등성 — 동일 idempotencyKey 재요청 시 같은 주문 반환`() {
        val accountId = createAccountAndDeposit()
        seedQuote("005930", "70000", "70100", "69900")
        val key = UUID.randomUUID().toString()
        val request = mapOf(
            "ticker" to "005930", "marketType" to "KOSPI",
            "orderType" to "LIMIT", "orderSide" to "BUY",
            "orderCondition" to "DAY", "quantity" to 1,
            "limitPrice" to 60000, "idempotencyKey" to key,
        )

        val first = mockMvc.post("/api/v1/accounts/$accountId/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect { status { isCreated() } }.andReturn()

        val second = mockMvc.post("/api/v1/accounts/$accountId/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect { status { isCreated() } }.andReturn()

        val firstId = objectMapper.readTree(first.response.contentAsString)["orderId"].asLong()
        val secondId = objectMapper.readTree(second.response.contentAsString)["orderId"].asLong()
        assertEquals(firstId, secondId)
    }
}