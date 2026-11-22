package com.papertrading.api.characterization.kis

import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.NotificationEventType
import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.enums.TransactionType
import com.papertrading.api.domain.port.CollectorSubscriptionPort
import com.papertrading.api.infrastructure.kis.KisTokenManager
import com.papertrading.api.infrastructure.persistence.AccountLedgerRepository
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.OrderRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestConstructor
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.web.client.RestTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal

/**
 * 특성화 시험(step 8, D21 b): KIS_PAPER·KIS_LIVE 주문 접수를 HTTP 입구부터 DB 와 KIS 호출까지 끝까지 통과시킨다.
 * KIS 응답은 MockRestServiceServer fixture, 토큰 관리자만 mock(+collector 구독 포트는 외부 호출 차단용).
 * 단언: KIS 요청(주소·tr_id·헤더·본문 전체), externalOrderId 저장, 주문·원장·잠금 행, Slack 이벤트 문구.
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
@Import(KisBoundaryEventCaptureConfig::class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@Suppress("LongParameterList") // 스프링 생성자 주입으로 저장소를 받는 시험 클래스라 매개변수 수를 줄일 수 없음
class KisOrderIntakeBoundaryCharacterizationTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val restTemplate: RestTemplate,
    private val accountRepository: AccountRepository,
    private val orderRepository: OrderRepository,
    private val positionRepository: PositionRepository,
    private val accountLedgerRepository: AccountLedgerRepository,
    private val eventCapture: KisBoundaryEventCapture,
) {
    @MockitoBean
    lateinit var kisTokenManager: KisTokenManager

    @MockitoBean
    lateinit var collectorSubscriptionPort: CollectorSubscriptionPort

    private lateinit var kisServer: MockRestServiceServer
    private lateinit var originalRequestFactory: ClientHttpRequestFactory

    // 브로커 쪽 기대값 한 묶음: 주소·인증 헤더 값·tr_id·본문 전체·돌려줄 주문번호
    private data class KisCall(
        val url: String,
        val credentials: Credentials,
        val trId: String,
        val body: String,
        val orderNo: String,
    )

    private data class Credentials(val token: String, val appKey: String, val appSecret: String)

    // 우리 쪽 입력 한 묶음
    private data class OrderIntent(
        val side: String,
        val type: String,
        val quantity: String,
        val limitPrice: String?,
        val key: String,
        val ticker: String = "005930",
    )

    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @Container
        @ServiceConnection(name = "redis")
        val redis = GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379)

        private const val PAPER_ORDER_URL =
            "https://openapivts.koreainvestment.com:29443/uapi/domestic-stock/v1/trading/order-cash"
        private const val LIVE_ORDER_URL =
            "https://openapi.koreainvestment.com:9443/uapi/domestic-stock/v1/trading/order-cash"
        private val PAPER = Credentials("paper-token", "paper-app-key", "paper-app-secret")
        private val LIVE = Credentials("live-token", "live-app-key", "live-app-secret")
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
    fun `KIS_PAPER 지정가 매수 접수는 VTTC0802U 로 모의 주소에 보내고 주문번호 저장과 매수 잠금 원장을 남긴다`() {
        val accountId = saveAccount("intake-paper-buy", TradingMode.KIS_PAPER, "12345678-01")
        val body = kisBody("12345678", "005930", "00", "10", "70000")
        expectKisCall(KisCall(PAPER_ORDER_URL, PAPER, "VTTC0802U", body, "0000117057"))

        val response = placeOrder(accountId, OrderIntent("BUY", "LIMIT", "10", "70000", "intake-paper-buy-1"))

        kisServer.verify()
        assertThat(normalizeOrderResponse(response))
            .isEqualTo(orderJson("005930", "LIMIT", "BUY", quantity = "10", limitPrice = "70000"))
        val orderId = orderIdOf(response)
        val order = orderRepository.findById(orderId).orElseThrow()
        assertThat(order.externalOrderId).isEqualTo("0000117057")
        assertThat(order.orderStatus).isEqualTo(OrderStatus.PENDING)
        assertThat(order.lockedAmount).isEqualByComparingTo("700000")
        assertThat(order.filledQuantity).isEqualByComparingTo("0")

        val account = accountRepository.findById(accountId).orElseThrow()
        assertThat(account.deposit).isEqualByComparingTo("10000000")
        assertThat(account.availableDeposit).isEqualByComparingTo("9300000")
        assertThat(account.lockedDeposit).isEqualByComparingTo("700000")

        val ledger = accountLedgerRepository.findByAccountIdOrderByCreatedAtDesc(accountId)
        assertThat(ledger).hasSize(1)
        assertThat(ledger[0].transactionType).isEqualTo(TransactionType.BUY_LOCK)
        assertThat(ledger[0].amount).isEqualByComparingTo("700000")
        assertThat(ledger[0].balanceAfter).isEqualByComparingTo("9300000")
        assertThat(ledger[0].refOrderId).isEqualTo(orderId)
        assertThat(ledger[0].refExecutionId).isNull()
        assertThat(ledger[0].idempotencyKey).isEqualTo("buy-lock-$orderId")

        assertSingleSlackMessage(
            accountId,
            orderId,
            "KIS_PAPER order created: orderId=$orderId, ticker=005930, externalOrderId=0000117057",
        )
        verify(collectorSubscriptionPort).subscribe("KIS_PAPER", "005930")
    }

    @Test
    fun `KIS_LIVE 지정가 매수 접수는 TTTC0802U 로 실전 주소에 보내고 주문번호 저장과 매수 잠금 원장을 남긴다`() {
        val accountId = saveAccount("intake-live-buy", TradingMode.KIS_LIVE, "87654321-01")
        val body = kisBody("87654321", "000660", "00", "3", "120000")
        expectKisCall(KisCall(LIVE_ORDER_URL, LIVE, "TTTC0802U", body, "0000228899"))

        val response = placeOrder(accountId, OrderIntent("BUY", "LIMIT", "3", "120000", "intake-live-buy-1", "000660"))

        kisServer.verify()
        assertThat(normalizeOrderResponse(response))
            .isEqualTo(orderJson("000660", "LIMIT", "BUY", quantity = "3", limitPrice = "120000"))
        val orderId = orderIdOf(response)
        val order = orderRepository.findById(orderId).orElseThrow()
        assertThat(order.externalOrderId).isEqualTo("0000228899")
        assertThat(order.lockedAmount).isEqualByComparingTo("360000")

        val account = accountRepository.findById(accountId).orElseThrow()
        assertThat(account.availableDeposit).isEqualByComparingTo("9640000")
        assertThat(account.lockedDeposit).isEqualByComparingTo("360000")

        val ledger = accountLedgerRepository.findByAccountIdOrderByCreatedAtDesc(accountId)
        assertThat(ledger).hasSize(1)
        assertThat(ledger[0].transactionType).isEqualTo(TransactionType.BUY_LOCK)
        assertThat(ledger[0].amount).isEqualByComparingTo("360000")
        assertThat(ledger[0].balanceAfter).isEqualByComparingTo("9640000")
        assertThat(ledger[0].idempotencyKey).isEqualTo("buy-lock-$orderId")

        assertSingleSlackMessage(
            accountId,
            orderId,
            "KIS_LIVE order created: orderId=$orderId, ticker=000660, externalOrderId=0000228899",
        )
        verify(collectorSubscriptionPort).subscribe("KIS_LIVE", "000660")
    }

    @Test
    fun `KIS_PAPER 시장가 매도 접수는 VTTC0801U 와 시장가 본문으로 보내고 보유 수량만 잠근다`() {
        val accountId = saveAccount("intake-paper-sell", TradingMode.KIS_PAPER, "12345678-01")
        saveHolding(accountId, "005930", quantity = "10", avgPrice = "65000")
        expectKisCall(
            KisCall(PAPER_ORDER_URL, PAPER, "VTTC0801U", kisBody("12345678", "005930", "01", "4", "0"), "0000331122"),
        )

        val response = placeOrder(accountId, OrderIntent("SELL", "MARKET", "4", null, "intake-paper-sell-1"))

        kisServer.verify()
        assertThat(normalizeOrderResponse(response))
            .isEqualTo(orderJson("005930", "MARKET", "SELL", quantity = "4", limitPrice = null))
        val orderId = orderIdOf(response)
        val order = orderRepository.findById(orderId).orElseThrow()
        assertThat(order.externalOrderId).isEqualTo("0000331122")
        assertThat(order.lockedAmount).isEqualByComparingTo("0")

        val position = positionRepository.findByAccountIdAndTicker(accountId, "005930").orElseThrow()
        assertThat(position.quantity).isEqualByComparingTo("10")
        assertThat(position.lockedQuantity).isEqualByComparingTo("4")
        assertThat(position.orderableQuantity).isEqualByComparingTo("6")

        val account = accountRepository.findById(accountId).orElseThrow()
        assertThat(account.availableDeposit).isEqualByComparingTo("10000000")
        assertThat(account.lockedDeposit).isEqualByComparingTo("0")
        assertThat(accountLedgerRepository.findByAccountIdOrderByCreatedAtDesc(accountId)).isEmpty()

        assertSingleSlackMessage(
            accountId,
            orderId,
            "KIS_PAPER order created: orderId=$orderId, ticker=005930, externalOrderId=0000331122",
        )
    }

    @Test
    fun `KIS_LIVE 지정가 매도 접수는 TTTC0801U 로 실전 주소에 보낸다`() {
        val accountId = saveAccount("intake-live-sell", TradingMode.KIS_LIVE, "87654321-01")
        saveHolding(accountId, "005930", quantity = "8", avgPrice = "65000")
        expectKisCall(
            KisCall(LIVE_ORDER_URL, LIVE, "TTTC0801U", kisBody("87654321", "005930", "00", "3", "72000"), "0000445566"),
        )

        val response = placeOrder(accountId, OrderIntent("SELL", "LIMIT", "3", "72000", "intake-live-sell-1"))

        kisServer.verify()
        val orderId = orderIdOf(response)
        assertThat(orderRepository.findById(orderId).orElseThrow().externalOrderId).isEqualTo("0000445566")
        val position = positionRepository.findByAccountIdAndTicker(accountId, "005930").orElseThrow()
        assertThat(position.lockedQuantity).isEqualByComparingTo("3")
        assertThat(position.orderableQuantity).isEqualByComparingTo("5")
        assertSingleSlackMessage(
            accountId,
            orderId,
            "KIS_LIVE order created: orderId=$orderId, ticker=005930, externalOrderId=0000445566",
        )
    }

    private fun expectKisCall(call: KisCall) {
        kisServer.expect(requestTo(call.url))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("authorization", "Bearer ${call.credentials.token}"))
            .andExpect(header("appkey", call.credentials.appKey))
            .andExpect(header("appsecret", call.credentials.appSecret))
            .andExpect(header("tr_id", call.trId))
            .andExpect(header("custtype", "P"))
            .andExpect(header("Content-Type", "application/json"))
            .andExpect(content().string(call.body))
            .andRespond(
                withSuccess(
                    """{"rt_cd":"0","msg_cd":"APBK0013","msg1":"주문 전송 완료 되었습니다.",""" +
                        """"output":{"ORNO":"${call.orderNo}"}}""",
                    MediaType.APPLICATION_JSON,
                ),
            )
    }

    // KIS 주문 본문 전체 문자열 (키 순서 포함 글자 그대로 비교)
    private fun kisBody(cano: String, ticker: String, ordDvsn: String, qty: String, unitPrice: String): String {
        val head = """{"CANO":"$cano","ACNT_PRDT_CD":"01","PDNO":"$ticker","ORD_DVSN":"$ordDvsn","""
        val tail = """"ORD_QTY":"$qty","ORD_UNPR":"$unitPrice"}"""
        return head + tail
    }

    // 접수 응답(201) 전체. 주문번호·생성 시각만 자리표시자
    private fun orderJson(ticker: String, type: String, side: String, quantity: String, limitPrice: String?): String {
        val head = """{"orderId":<ID>,"ticker":"$ticker","marketType":"KOSPI","""
        val kind = """"orderType":"$type","orderSide":"$side","""
        val middle = """"orderCondition":"DAY","orderStatus":"PENDING","quantity":$quantity,"filledQuantity":0,"""
        val tail = """"limitPrice":$limitPrice,"avgFilledPrice":null,"fee":0,"createdAt":"<AT>"}"""
        return head + kind + middle + tail
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

    private fun saveHolding(accountId: Long, ticker: String, quantity: String, avgPrice: String) {
        val account = accountRepository.getReferenceById(accountId)
        positionRepository.save(
            Position.createWithHolding(account, ticker, MarketType.KOSPI, BigDecimal(quantity), BigDecimal(avgPrice)),
        )
    }

    private fun placeOrder(accountId: Long, intent: OrderIntent): String {
        val request = mapOf(
            "ticker" to intent.ticker,
            "marketType" to "KOSPI",
            "orderType" to intent.type,
            "orderSide" to intent.side,
            "orderCondition" to "DAY",
            "quantity" to BigDecimal(intent.quantity),
            "limitPrice" to intent.limitPrice?.let { BigDecimal(it) },
            "idempotencyKey" to intent.key,
        )
        return mockMvc.post("/api/v1/accounts/$accountId/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString
    }

    private fun orderIdOf(response: String): Long = objectMapper.readTree(response).path("orderId").asLong()

    // 주문번호·생성 시각은 실행마다 달라 모양만 남기고 나머지 필드는 글자 그대로 비교
    private fun normalizeOrderResponse(response: String): String = response
        .replace(Regex(""""orderId":\d+"""), """"orderId":<ID>""")
        .replace(Regex(""""createdAt":"[^"]+""""), """"createdAt":"<AT>"""")

    private fun assertSingleSlackMessage(accountId: Long, orderId: Long, message: String) {
        val events = eventCapture.slackEventsOf(accountId)
        assertThat(events).hasSize(1)
        assertThat(events[0].eventType).isEqualTo(NotificationEventType.ORDER_CREATED)
        assertThat(events[0].orderId).isEqualTo(orderId)
        assertThat(events[0].executionId).isNull()
        assertThat(events[0].message).isEqualTo(message)
    }
}
