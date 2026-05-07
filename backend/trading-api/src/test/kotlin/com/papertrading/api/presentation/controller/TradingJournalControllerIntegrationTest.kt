package com.papertrading.api.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class TradingJournalControllerIntegrationTest {
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
    fun `create list detail update 흐름이 동작한다`() {
        val accountCreateResult = mockMvc.post("/api/v1/accounts") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "accountName" to "journal-account",
                    "accountType" to "STOCK",
                    "tradingMode" to "LOCAL",
                    "initialDeposit" to 1000000
                )
            )
        }.andExpect { status { isCreated() } }.andReturn()
        val accountId = objectMapper.readTree(accountCreateResult.response.contentAsString)["id"].asLong()

        val createResult = mockMvc.post("/api/trading-journals") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "accountId" to accountId,
                    "journalType" to "MANUAL",
                    "title" to "첫 일지",
                    "content" to "테스트",
                    "ticker" to "005930",
                    "sentiment" to "BULLISH"
                )
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.accountId") { value(accountId) }
        }.andReturn()
        val journalId = objectMapper.readTree(createResult.response.contentAsString)["id"].asLong()

        mockMvc.get("/api/trading-journals") {
            param("accountId", accountId.toString())
            param("ticker", "005930")
            param("page", "0")
            param("size", "20")
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(1) }
        }

        mockMvc.get("/api/trading-journals/$journalId") {
            param("accountId", accountId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(journalId) }
        }

        mockMvc.patch("/api/trading-journals/$journalId") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "accountId" to accountId,
                    "title" to "수정",
                    "content" to "수정본문",
                    "sentiment" to "NEUTRAL"
                )
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$.title") { value("수정") }
        }
    }

    @Test
    fun `create 요청이 잘못되면 400을 반환한다`() {
        mockMvc.post("/api/trading-journals") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "journalType" to "",
                    "title" to "",
                    "content" to ""
                )
            )
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `invalid sentiment로 create 요청하면 400을 반환한다`() {
        val accountId = createAccount("invalid-sentiment")

        mockMvc.post("/api/trading-journals") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "accountId" to accountId,
                    "journalType" to "MANUAL",
                    "title" to "sentiment test",
                    "content" to "invalid sentiment",
                    "sentiment" to "INVALID"
                )
            )
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `존재하지 않는 accountId로 조회하면 404를 반환한다`() {
        mockMvc.get("/api/trading-journals") {
            param("accountId", "999999")
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `journal account 불일치면 detail이 404를 반환한다`() {
        val accountA = createAccount("a")
        val accountB = createAccount("b")
        val journalId = createJournal(accountA, "005930")

        mockMvc.get("/api/trading-journals/$journalId") {
            param("accountId", accountB.toString())
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `ticker 필터와 paging이 적용되고 최신순으로 정렬된다`() {
        val accountId = createAccount("paging")
        createJournal(accountId, "005930", "title-005930-1")
        createJournal(accountId, "005930", "title-005930-2")
        createJournal(accountId, "000660", "title-000660-1")

        mockMvc.get("/api/trading-journals") {
            param("accountId", accountId.toString())
            param("ticker", "005930")
            param("page", "0")
            param("size", "1")
        }.andExpect {
            status { isOk() }
            jsonPath("$.items", hasSize<Any>(1))
            jsonPath("$.totalElements") { value(2) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.items[0].title") { value("title-005930-2") }
        }
    }

    @Test
    fun `invalid size로 목록 조회하면 400을 반환한다`() {
        val accountId = createAccount("invalid-size")

        mockMvc.get("/api/trading-journals") {
            param("accountId", accountId.toString())
            param("page", "0")
            param("size", "0")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    private fun createAccount(name: String): Long {
        val accountCreateResult = mockMvc.post("/api/v1/accounts") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "accountName" to "journal-$name",
                    "accountType" to "STOCK",
                    "tradingMode" to "LOCAL",
                    "initialDeposit" to 1000000
                )
            )
        }.andExpect {
            status { isCreated() }
        }.andReturn()
        return objectMapper.readTree(accountCreateResult.response.contentAsString)["id"].asLong()
    }

    private fun createJournal(accountId: Long, ticker: String): Long {
        return createJournal(accountId, ticker, "title-$ticker")
    }

    private fun createJournal(accountId: Long, ticker: String, title: String): Long {
        val createResult = mockMvc.post("/api/trading-journals") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "accountId" to accountId,
                    "journalType" to "MANUAL",
                    "title" to title,
                    "content" to "content-$ticker",
                    "ticker" to ticker
                )
            )
        }.andExpect {
            status { isCreated() }
        }.andReturn()
        return objectMapper.readTree(createResult.response.contentAsString)["id"].asLong()
    }
}
