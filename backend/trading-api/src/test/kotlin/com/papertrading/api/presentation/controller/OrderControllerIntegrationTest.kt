package com.papertrading.api.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.api.application.order.LocalMatchingEngine
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.port.CollectorSubscriptionPort
import com.papertrading.api.application.common.result.QuoteSnapshot
import com.papertrading.api.infrastructure.kis.KisOrderRestClient
import com.papertrading.api.infrastructure.persistence.AccountLedgerRepository
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.OrderRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.mockito.Mockito
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

    @Autowired lateinit var accountRepository: AccountRepository

    @Autowired lateinit var orderRepository: OrderRepository

    @Autowired lateinit var accountLedgerRepository: AccountLedgerRepository

    @Autowired lateinit var positionRepository: PositionRepository

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
        val quote = QuoteSnapshot("005930", TradingMode.LOCAL, BigDecimal("70000"), BigDecimal("70100"), BigDecimal("69900"), Instant.now())
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
        }.andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("QUOTE_UNAVAILABLE") }
        }
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
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("POSITION_NOT_FOUND") }
        }
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

    // ---- 에러 정렬(DEC-02): 경로마다 범주 상태와 code 로 응답 ----

    private fun orderRequest(
        orderType: String = "LIMIT",
        orderSide: String = "BUY",
        orderCondition: String = "DAY",
        quantity: Int = 1,
        limitPrice: Int? = 60000,
    ): Map<String, Any> = buildMap {
        put("ticker", "005930")
        put("marketType", "KOSPI")
        put("orderType", orderType)
        put("orderSide", orderSide)
        put("orderCondition", orderCondition)
        put("quantity", quantity)
        if (limitPrice != null) put("limitPrice", limitPrice)
        put("idempotencyKey", UUID.randomUUID().toString())
    }

    // 거부 뒤 주문 행 수, 원장 행 수, 예수금, 잠금, 포지션 불변까지 단언 (S3Q-03)
    private fun expectOrderError(accountId: Long, body: Map<String, Any>, httpStatus: Int, code: String) {
        val before = footprint(accountId)

        mockMvc.post("/api/v1/accounts/$accountId/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }.andExpect {
            status { isEqualTo(httpStatus) }
            jsonPath("$.status") { value(httpStatus) }
            jsonPath("$.code") { value(code) }
        }

        assertFootprintUnchanged(before, footprint(accountId))
    }

    @Test
    fun `지정가 주문에 가격이 없으면 400 LIMIT_PRICE_REQUIRED`() {
        val accountId = createAccountAndDeposit()

        expectOrderError(accountId, orderRequest(limitPrice = null), 400, "LIMIT_PRICE_REQUIRED")
    }

    @Test
    fun `GTD 주문에 만료 시각이 없으면 400 EXPIRE_AT_REQUIRED`() {
        val accountId = createAccountAndDeposit()

        expectOrderError(accountId, orderRequest(orderCondition = "GTD"), 400, "EXPIRE_AT_REQUIRED")
    }

    @Test
    fun `가용 예수금보다 큰 매수 주문은 409 INSUFFICIENT_AVAILABLE_DEPOSIT`() {
        val accountId = createAccountAndDeposit(deposit = 1_000)

        expectOrderError(accountId, orderRequest(limitPrice = 70000), 409, "INSUFFICIENT_AVAILABLE_DEPOSIT")
    }

    @Test
    fun `비활성화된 계좌의 주문은 409 ACCOUNT_INACTIVE`() {
        val accountId = createAccountAndDeposit()
        mockMvc.delete("/api/v1/accounts/$accountId").andExpect { status { isNoContent() } }

        expectOrderError(accountId, orderRequest(), 409, "ACCOUNT_INACTIVE")
    }

    @Test
    fun `보유 수량보다 많은 매도 주문은 409 INSUFFICIENT_ORDERABLE_QUANTITY`() {
        val accountId = createAccountAndDeposit()
        positionRepository.save(
            Position.createWithHolding(
                account = accountRepository.findById(accountId).orElseThrow(),
                ticker = "005930",
                marketType = MarketType.KOSPI,
                quantity = BigDecimal("2"),
                avgBuyPrice = BigDecimal("60000"),
            ),
        )

        expectOrderError(
            accountId,
            orderRequest(orderSide = "SELL", quantity = 3, limitPrice = 75000),
            409,
            "INSUFFICIENT_ORDERABLE_QUANTITY",
        )
    }

    private fun placePendingBuyOrder(accountId: Long): Long {
        seedQuote("005930", "70000", "70100", "69900")
        val result = mockMvc.post("/api/v1/accounts/$accountId/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(orderRequest(limitPrice = 60000))
        }.andExpect { status { isCreated() } }.andReturn()
        return objectMapper.readTree(result.response.contentAsString)["orderId"].asLong()
    }

    @Test
    fun `다른 계좌의 주문 취소는 404 ORDER_NOT_FOUND`() {
        val ownerId = createAccountAndDeposit()
        val otherId = createAccountAndDeposit()
        val orderId = placePendingBuyOrder(ownerId)
        val ownerBefore = footprint(ownerId)
        val otherBefore = footprint(otherId)

        mockMvc.delete("/api/v1/accounts/$otherId/orders/$orderId").andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("ORDER_NOT_FOUND") }
        }

        assertFootprintUnchanged(ownerBefore, footprint(ownerId))
        assertFootprintUnchanged(otherBefore, footprint(otherId))
        expectOrderStatus(ownerId, orderId, "PENDING")
    }

    @Test
    fun `이미 취소된 주문을 다시 취소하면 409 ORDER_NOT_CANCELABLE`() {
        val accountId = createAccountAndDeposit()
        val orderId = placePendingBuyOrder(accountId)
        mockMvc.delete("/api/v1/accounts/$accountId/orders/$orderId").andExpect { status { isNoContent() } }
        val afterFirstCancel = footprint(accountId)

        mockMvc.delete("/api/v1/accounts/$accountId/orders/$orderId").andExpect {
            status { isConflict() }
            jsonPath("$.status") { value(409) }
            jsonPath("$.code") { value("ORDER_NOT_CANCELABLE") }
        }

        assertFootprintUnchanged(afterFirstCancel, footprint(accountId))
        expectOrderStatus(accountId, orderId, "CANCELLED")
    }

    private fun expectOrderStatus(accountId: Long, orderId: Long, orderStatus: String) {
        mockMvc.get("/api/v1/accounts/$accountId/orders/$orderId").andExpect {
            status { isOk() }
            jsonPath("$.orderStatus") { value(orderStatus) }
        }
    }

    // ---- TOSS_LIVE, UPBIT_LIVE: 브로커 주문 연동이 없어 501 로 거부, 주문·원장·잠금 불변 ----

    private fun createBrokerAccount(accountType: String, tradingMode: String): Long {
        val result = mockMvc.post("/api/v1/accounts") {
            contentType = MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(
                    mapOf(
                        "accountName" to "브로커 $tradingMode",
                        "accountType" to accountType,
                        "tradingMode" to tradingMode,
                        "initialDeposit" to 10_000_000,
                    ),
                )
        }.andExpect { status { isCreated() } }.andReturn()
        return objectMapper.readTree(result.response.contentAsString)["id"].asLong()
    }

    private fun orderBody(
        orderType: String,
        orderSide: String,
        orderCondition: String = "DAY",
        quantity: Int = 5,
    ): String {
        val body =
            buildMap<String, Any> {
                put("ticker", "005930")
                put("marketType", "KOSPI")
                put("orderType", orderType)
                put("orderSide", orderSide)
                put("orderCondition", orderCondition)
                put("quantity", quantity)
                if (orderType == "LIMIT") put("limitPrice", 70000)
                put("idempotencyKey", UUID.randomUUID().toString())
            }
        return objectMapper.writeValueAsString(body)
    }

    private data class AccountFootprint(
        val orderCount: Int,
        val ledgerCount: Int,
        val availableDeposit: BigDecimal,
        val lockedDeposit: BigDecimal,
        val positionLockedQuantity: BigDecimal?,
        val positionQuantity: BigDecimal?,
    )

    private fun footprint(accountId: Long): AccountFootprint {
        val account = accountRepository.findById(accountId).orElseThrow()
        val position = positionRepository.findByAccountIdAndTicker(accountId, "005930").orElse(null)
        return AccountFootprint(
            orderCount = orderRepository.findByAccountIdOrderByCreatedAtDesc(accountId).size,
            ledgerCount = accountLedgerRepository.findByAccountIdOrderByCreatedAtDesc(accountId).size,
            availableDeposit = account.availableDeposit,
            lockedDeposit = account.lockedDeposit,
            positionLockedQuantity = positionRepository.findByAccountIdAndTicker(accountId, "005930")
                .map { it.lockedQuantity }.orElse(null),
            positionQuantity = position?.quantity,
        )
    }

    // 금액·수량은 compareTo 비교, 포지션 없음(null)은 그대로 null 이어야 함
    private fun assertFootprintUnchanged(before: AccountFootprint, after: AccountFootprint) {
        assertEquals(before.orderCount, after.orderCount, "주문 행 수")
        assertEquals(before.ledgerCount, after.ledgerCount, "원장 행 수")
        assertEquals(0, before.availableDeposit.compareTo(after.availableDeposit), "가용 예수금")
        assertEquals(0, before.lockedDeposit.compareTo(after.lockedDeposit), "잠금 예수금")
        assertSameAmount(before.positionQuantity, after.positionQuantity, "포지션 수량")
        assertSameAmount(before.positionLockedQuantity, after.positionLockedQuantity, "포지션 잠금 수량")
    }

    private fun assertSameAmount(before: BigDecimal?, after: BigDecimal?, label: String) {
        if (before == null || after == null) {
            assertEquals(before, after, label)
        } else {
            assertEquals(0, before.compareTo(after), label)
        }
    }

    private fun assertOrderRejectedWithoutChange(accountId: Long, body: String) {
        val before = footprint(accountId)

        mockMvc.post("/api/v1/accounts/$accountId/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isNotImplemented() }
            jsonPath("$.status") { value(501) }
            jsonPath("$.code") { value("BROKER_ORDER_NOT_SUPPORTED") }
        }

        val after = footprint(accountId)
        assertEquals(before.orderCount, after.orderCount)
        assertEquals(before.ledgerCount, after.ledgerCount)
        assertEquals(0, before.availableDeposit.compareTo(after.availableDeposit))
        assertEquals(0, before.lockedDeposit.compareTo(after.lockedDeposit))
        assertEquals(0, BigDecimal.ZERO.compareTo(after.lockedDeposit))
        assertEquals(before.positionLockedQuantity, after.positionLockedQuantity)
        Mockito.verifyNoInteractions(collectorSubscriptionPort)
    }

    @Test
    fun `TOSS_LIVE 지정가 매수 주문은 501 BROKER_ORDER_NOT_SUPPORTED, 주문·원장·잠금 불변`() {
        val accountId = createBrokerAccount("STOCK", "TOSS_LIVE")

        assertOrderRejectedWithoutChange(accountId, orderBody("LIMIT", "BUY"))
    }

    @Test
    fun `TOSS_LIVE 시장가 매수 주문은 시세가 없어도 501 로 거부`() {
        val accountId = createBrokerAccount("STOCK", "TOSS_LIVE")

        assertOrderRejectedWithoutChange(accountId, orderBody("MARKET", "BUY"))
    }

    @Test
    fun `TOSS_LIVE IOC 주문은 취소 주문을 남기지 않고 501 로 거부`() {
        val accountId = createBrokerAccount("STOCK", "TOSS_LIVE")

        assertOrderRejectedWithoutChange(accountId, orderBody("LIMIT", "BUY", orderCondition = "IOC"))
    }

    @Test
    fun `TOSS_LIVE 매도 주문은 포지션 수량을 잠그지 않고 501 로 거부`() {
        val accountId = createBrokerAccount("STOCK", "TOSS_LIVE")
        val account = accountRepository.findById(accountId).orElseThrow()
        positionRepository.save(
            Position.createWithHolding(
                account = account,
                ticker = "005930",
                marketType = MarketType.KOSPI,
                quantity = BigDecimal("10"),
                avgBuyPrice = BigDecimal("60000"),
            ),
        )

        assertOrderRejectedWithoutChange(accountId, orderBody("LIMIT", "SELL"))
    }

    @Test
    fun `UPBIT_LIVE 지정가 매수 주문도 501 BROKER_ORDER_NOT_SUPPORTED, 주문·원장·잠금 불변`() {
        val accountId = createBrokerAccount("CRYPTO", "UPBIT_LIVE")

        assertOrderRejectedWithoutChange(accountId, orderBody("LIMIT", "BUY"))
    }
}
