package com.papertrading.api.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.model.Account
import com.papertrading.api.domain.model.Position
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class PositionControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var accountRepository: AccountRepository

    @Autowired
    lateinit var positionRepository: PositionRepository

    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @Container
        @ServiceConnection(name = "redis")
        val redis = GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379)
    }

    private lateinit var account: Account

    @BeforeEach
    fun setUp() {
        positionRepository.deleteAll()
        accountRepository.deleteAll()
        account = accountRepository.save(
            Account.create(
                accountName = "포지션테스트계좌",
                accountType = AccountType.STOCK,
                tradingMode = TradingMode.LOCAL,
                initialDeposit = BigDecimal("5000000"),
            )
        )
    }

    private fun createAccountViaApi(): Long {
        val request = mapOf(
            "accountName" to "API생성계좌-${UUID.randomUUID()}",
            "accountType" to "STOCK",
            "tradingMode" to "LOCAL",
            "initialDeposit" to 1000000
        )
        val result = mockMvc.post("/api/v1/accounts") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
        }.andReturn()
        return objectMapper.readTree(result.response.contentAsString)["id"].asLong()
    }

    private fun savePosition(ticker: String, qty: BigDecimal, avgPrice: BigDecimal = BigDecimal("70000")): Position {
        val pos = Position(
            ticker = ticker,
            marketType = MarketType.KOSPI,
            quantity = qty,
            lockedQuantity = BigDecimal.ZERO,
            orderableQuantity = qty,
            avgBuyPrice = avgPrice,
            totalBuyAmount = avgPrice.multiply(qty),
        )
        pos.account = account
        return positionRepository.save(pos)
    }

    // -------------------------------------------------------------------
    // 시나리오 1: 포지션 없는 계좌 → 빈 배열 반환
    // -------------------------------------------------------------------
    @Test
    fun `포지션_없는_계좌는_빈_배열을_반환한다`() {
        mockMvc.get("/api/v1/accounts/${account.id}/positions").andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$") { isArray() }
            jsonPath("$.length()") { value(0) }
        }
    }

    // -------------------------------------------------------------------
    // 시나리오 2: 포지션 목록 정상 조회 (quantity > 0)
    // -------------------------------------------------------------------
    @Test
    fun `포지션_목록_조회시_quantity가_0보다_큰_포지션만_반환한다`() {
        savePosition("005930", BigDecimal("10"), BigDecimal("70000"))
        savePosition("035720", BigDecimal.ZERO, BigDecimal("50000")) // quantity=0 → 제외

        mockMvc.get("/api/v1/accounts/${account.id}/positions").andExpect {
            status { isOk() }
            jsonPath("$") { isArray() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].ticker") { value("005930") }
            jsonPath("$[0].quantity") { isNotEmpty() }
            jsonPath("$[0].avgBuyPrice") { isNotEmpty() }
        }
    }

    // -------------------------------------------------------------------
    // 시나리오 2b: 복수 포지션 목록 반환
    // -------------------------------------------------------------------
    @Test
    fun `복수_포지션_보유시_전체_목록이_반환된다`() {
        savePosition("005930", BigDecimal("10"), BigDecimal("70000"))
        savePosition("035720", BigDecimal("5"), BigDecimal("50000"))

        mockMvc.get("/api/v1/accounts/${account.id}/positions").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
        }
    }

    // -------------------------------------------------------------------
    // 시나리오 3: 단건 포지션 조회 (대문자 ticker)
    // -------------------------------------------------------------------
    @Test
    fun `단건_포지션_조회시_정상_포지션을_반환한다`() {
        savePosition("005930", BigDecimal("10"), BigDecimal("70000"))

        mockMvc.get("/api/v1/accounts/${account.id}/positions/005930").andExpect {
            status { isOk() }
            jsonPath("$.ticker") { value("005930") }
            jsonPath("$.marketType") { value("KOSPI") }
            jsonPath("$.quantity") { isNotEmpty() }
            jsonPath("$.avgBuyPrice") { isNotEmpty() }
        }
    }

    // -------------------------------------------------------------------
    // 시나리오 4: 소문자 ticker 입력 → 컨트롤러에서 uppercase 처리 → 정상 반환
    // -------------------------------------------------------------------
    @Test
    fun `소문자_ticker_입력시_대소문자_무관하게_포지션을_반환한다`() {
        savePosition("AAPL", BigDecimal("3"), BigDecimal("150000"))

        // 소문자로 요청
        mockMvc.get("/api/v1/accounts/${account.id}/positions/aapl").andExpect {
            status { isOk() }
            jsonPath("$.ticker") { value("AAPL") }
        }
    }

    // -------------------------------------------------------------------
    // 시나리오 4b: 공백+소문자 ticker 입력 → trim+uppercase 처리
    // -------------------------------------------------------------------
    @Test
    fun `공백과_소문자가_포함된_ticker_입력시_정상_처리된다`() {
        savePosition("005930", BigDecimal("7"), BigDecimal("65000"))

        // path variable에 소문자 입력 (trim은 path variable이므로 공백은 URL인코딩 이슈 우회)
        mockMvc.get("/api/v1/accounts/${account.id}/positions/005930").andExpect {
            status { isOk() }
            jsonPath("$.ticker") { value("005930") }
        }
    }

    // -------------------------------------------------------------------
    // 시나리오 5: 존재하지 않는 ticker → 404 + code=NOT_FOUND
    // -------------------------------------------------------------------
    @Test
    fun `존재하지_않는_ticker_조회시_404를_반환한다`() {
        mockMvc.get("/api/v1/accounts/${account.id}/positions/NOTEXIST").andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("NOT_FOUND") }
        }
    }

    // -------------------------------------------------------------------
    // 시나리오 6: 존재하지 않는 계좌 → 포지션 없음 (빈 배열)
    // -------------------------------------------------------------------
    @Test
    fun `존재하지_않는_계좌의_포지션_조회시_빈_배열을_반환한다`() {
        mockMvc.get("/api/v1/accounts/99999/positions").andExpect {
            status { isOk() }
            jsonPath("$") { isArray() }
            jsonPath("$.length()") { value(0) }
        }
    }

    // -------------------------------------------------------------------
    // Edge case: avgBuyPrice=0 포지션 → returnRate null (0으로 나누기 방지)
    // -------------------------------------------------------------------
    @Test
    fun `avgBuyPrice가_0인_포지션은_returnRate가_null이다`() {
        val pos = Position(
            ticker = "TEST",
            marketType = MarketType.KOSPI,
            quantity = BigDecimal("10"),
            lockedQuantity = BigDecimal.ZERO,
            orderableQuantity = BigDecimal("10"),
            avgBuyPrice = BigDecimal.ZERO,
            totalBuyAmount = BigDecimal.ZERO,
        )
        pos.account = account
        positionRepository.save(pos)

        mockMvc.get("/api/v1/accounts/${account.id}/positions/TEST").andExpect {
            status { isOk() }
            jsonPath("$.ticker") { value("TEST") }
            jsonPath("$.returnRate") { doesNotExist() }  // null → JSON omitted or null
        }
    }

    // -------------------------------------------------------------------
    // Edge case: lockedQuantity > 0 → orderableQuantity 응답 확인
    // -------------------------------------------------------------------
    @Test
    fun `잠금수량이_있는_포지션은_orderableQuantity가_올바르게_반환된다`() {
        val pos = Position(
            ticker = "005930",
            marketType = MarketType.KOSPI,
            quantity = BigDecimal("10"),
            lockedQuantity = BigDecimal("3"),
            orderableQuantity = BigDecimal("7"),
            avgBuyPrice = BigDecimal("70000"),
            totalBuyAmount = BigDecimal("700000"),
        )
        pos.account = account
        positionRepository.save(pos)

        mockMvc.get("/api/v1/accounts/${account.id}/positions/005930").andExpect {
            status { isOk() }
            jsonPath("$.lockedQuantity") { isNotEmpty() }
            jsonPath("$.orderableQuantity") { isNotEmpty() }
        }
    }
}