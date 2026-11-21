package com.papertrading.api.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TradingMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
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
class AccountControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @Container
        @ServiceConnection(name = "redis")
        val redis = GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379)
    }

    @Test
    fun `계좌를_생성하면_201과_생성된_계좌를_반환한다`() {
        val request = mapOf(
            "accountName" to "테스트계좌",
            "accountType" to "STOCK",
            "tradingMode" to "LOCAL",
            "initialDeposit" to 1000000
        )

        mockMvc.post("/api/v1/accounts") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.accountName") { value("테스트계좌") }
            jsonPath("$.deposit") { value(1000000) }
            jsonPath("$.isActive") { value(true) }
            jsonPath("$.source") { value("LOCAL") }
        }
    }

    @Test
    fun `계좌_단건과_목록_응답의_source_는_계좌_모드에서_나온다`() {
        val cases = listOf(
            Triple("LOCAL", "STOCK", "LOCAL"),
            Triple("KIS_PAPER", "STOCK", "KIS"),
            Triple("KIS_LIVE", "STOCK", "KIS"),
            Triple("TOSS_LIVE", "STOCK", "TOSS"),
            Triple("UPBIT_LIVE", "CRYPTO", "UPBIT"),
        )
        assertEquals(TradingMode.entries.map { it.name }.toSet(), cases.map { it.first }.toSet())
        cases.forEach { (mode, type, source) ->
            val createResult = mockMvc.post("/api/v1/accounts") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "accountName" to "출처-$mode-${UUID.randomUUID()}",
                        "accountType" to type,
                        "tradingMode" to mode,
                        "initialDeposit" to 1000,
                    ),
                )
            }.andExpect {
                status { isCreated() }
                jsonPath("$.source") { value(source) }
            }.andReturn()
            val accountId = objectMapper.readTree(createResult.response.contentAsString)["id"].asLong()

            mockMvc.get("/api/v1/accounts/$accountId").andExpect {
                status { isOk() }
                jsonPath("$.source") { value(source) }
            }
            mockMvc.get("/api/v1/accounts").andExpect {
                status { isOk() }
                jsonPath("$[?(@.id == $accountId)].source") { value(source) }
            }
        }
    }

    @Test
    fun `계좌_조회_입금_출금_원장조회_전체_흐름이_정상_동작한다`() {
        // 계좌 생성
        val createRequest = mapOf(
            "accountName" to "흐름테스트",
            "accountType" to "STOCK",
            "tradingMode" to "LOCAL",
            "initialDeposit" to 500000
        )
        val createResult = mockMvc.post("/api/v1/accounts") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
        }.andExpect {
            status { isCreated() }
        }.andReturn()

        val accountId = objectMapper.readTree(createResult.response.contentAsString)["id"].asLong()

        // 입금
        val depositRequest = mapOf(
            "amount" to 100000,
            "idempotencyKey" to UUID.randomUUID().toString()
        )
        mockMvc.post("/api/v1/accounts/$accountId/deposit") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(depositRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.transactionType") { value("DEPOSIT") }
            jsonPath("$.balanceAfter") { value(600000) }
        }

        // 출금
        val withdrawRequest = mapOf(
            "amount" to 50000,
            "idempotencyKey" to UUID.randomUUID().toString()
        )
        mockMvc.post("/api/v1/accounts/$accountId/withdraw") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(withdrawRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.transactionType") { value("WITHDRAWAL") }
            jsonPath("$.balanceAfter") { value(550000) }
        }

        // 계좌 상세 조회
        mockMvc.get("/api/v1/accounts/$accountId").andExpect {
            status { isOk() }
            jsonPath("$.deposit") { value(550000) }
        }

        // 원장 조회
        mockMvc.get("/api/v1/accounts/$accountId/ledgers").andExpect {
            status { isOk() }
            jsonPath("$.totalElements") { value(3) } // 초기입금 + 입금 + 출금
        }
    }

    @Test
    fun `동일한_idempotency_key로_재입금해도_한번만_처리된다`() {
        val createRequest = mapOf(
            "accountName" to "멱등테스트",
            "accountType" to "STOCK",
            "tradingMode" to "LOCAL",
            "initialDeposit" to 0
        )
        val createResult = mockMvc.post("/api/v1/accounts") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
        }.andReturn()

        val accountId = objectMapper.readTree(createResult.response.contentAsString)["id"].asLong()
        val idempotencyKey = UUID.randomUUID().toString()

        val depositRequest = mapOf("amount" to 100000, "idempotencyKey" to idempotencyKey)

        // 첫 번째 요청
        mockMvc.post("/api/v1/accounts/$accountId/deposit") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(depositRequest)
        }.andExpect { status { isOk() } }

        // 동일 키 재요청
        mockMvc.post("/api/v1/accounts/$accountId/deposit") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(depositRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.balanceAfter") { value(100000) } // 중복 입금 없음
        }
    }

    @Test
    fun `존재하지_않는_계좌_조회시_404를_반환한다`() {
        mockMvc.get("/api/v1/accounts/99999").andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("ACCOUNT_NOT_FOUND") }
        }
    }

    @Test
    fun `가용_예수금_초과_출금시_409_INSUFFICIENT_AVAILABLE_DEPOSIT을_반환한다`() {
        val createRequest = mapOf(
            "accountName" to "부족테스트",
            "accountType" to "STOCK",
            "tradingMode" to "LOCAL",
            "initialDeposit" to 10000
        )
        val createResult = mockMvc.post("/api/v1/accounts") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
        }.andReturn()

        val accountId = objectMapper.readTree(createResult.response.contentAsString)["id"].asLong()

        mockMvc.post("/api/v1/accounts/$accountId/withdraw") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("amount" to 99999999, "idempotencyKey" to UUID.randomUUID().toString())
            )
        }.andExpect {
            status { isConflict() }
            jsonPath("$.status") { value(409) }
            jsonPath("$.code") { value("INSUFFICIENT_AVAILABLE_DEPOSIT") }
        }
    }

    @Test
    fun `계좌를_비활성화하면_204를_반환한다`() {
        val createRequest = mapOf(
            "accountName" to "삭제테스트",
            "accountType" to "STOCK",
            "tradingMode" to "LOCAL",
            "initialDeposit" to 0
        )
        val createResult = mockMvc.post("/api/v1/accounts") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
        }.andReturn()

        val accountId = objectMapper.readTree(createResult.response.contentAsString)["id"].asLong()

        mockMvc.delete("/api/v1/accounts/$accountId").andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `이미_비활성화된_계좌를_다시_비활성화하면_409_ACCOUNT_ALREADY_INACTIVE를_반환한다`() {
        val createResult = mockMvc.post("/api/v1/accounts") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "accountName" to "이중삭제테스트",
                    "accountType" to "STOCK",
                    "tradingMode" to "LOCAL",
                    "initialDeposit" to 0,
                ),
            )
        }.andReturn()
        val accountId = objectMapper.readTree(createResult.response.contentAsString)["id"].asLong()
        mockMvc.delete("/api/v1/accounts/$accountId").andExpect { status { isNoContent() } }

        mockMvc.delete("/api/v1/accounts/$accountId").andExpect {
            status { isConflict() }
            jsonPath("$.status") { value(409) }
            jsonPath("$.code") { value("ACCOUNT_ALREADY_INACTIVE") }
        }
    }
}
