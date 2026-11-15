package com.papertrading.api.presentation.controller

import com.papertrading.api.application.toss.TossAccountListQueryService
import com.papertrading.api.application.toss.port.TossAccountSummary
import com.papertrading.api.common.exception.TossAuthFailedException
import com.papertrading.api.common.exception.TossIntegrationDisabledException
import com.papertrading.api.common.exception.TossIpNotAllowedException
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willThrow
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

// GET /api/toss/accounts: 운영자가 accountSeq 를 확인하는 목록, accountNo 는 끝 4자리만 (D3, D9, 사람 결정 2026-11-15)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class TossAccountControllerIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc

    @MockitoBean lateinit var tossAccountListQueryService: TossAccountListQueryService

    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @Container
        @ServiceConnection(name = "redis")
        val redis = GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379)
    }

    @Test
    fun `정상 조회는 200 이고 accountNo 는 끝 4자리 accountSeq 는 전체 값 문자열이다`() {
        given(tossAccountListQueryService.listAccounts()).willReturn(
            listOf(
                TossAccountSummary("8901", 1L, "BROKERAGE"),
                TossAccountSummary("2100", 9_876_543_210L, "PENSION_SAVINGS"),
            ),
        )

        mockMvc.get("/api/toss/accounts")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].accountNoLast4") { value("8901") }
                jsonPath("$[0].accountSeq") { value("1") }
                jsonPath("$[0].accountType") { value("BROKERAGE") }
                jsonPath("$[1].accountNoLast4") { value("2100") }
                jsonPath("$[1].accountSeq") { value("9876543210") }
                jsonPath("$[1].accountType") { value("PENSION_SAVINGS") }
                jsonPath("$[0].accountNo") { doesNotExist() }
            }
    }

    @Test
    fun `계좌가 없으면 빈 배열이다`() {
        given(tossAccountListQueryService.listAccounts()).willReturn(emptyList())

        mockMvc.get("/api/toss/accounts").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(0) }
        }
    }

    @Test
    fun `토스 실패는 범주별 상태와 code 로 매핑된다`() {
        // 이미 예외를 던지도록 정한 목을 다시 정할 때 given(...) 호출이 예외를 던지므로 willThrow(...).given(...) 형태로 정함
        willThrow(TossIpNotAllowedException("403")).given(tossAccountListQueryService).listAccounts()
        mockMvc.get("/api/toss/accounts").andExpect {
            status { isServiceUnavailable() }
            jsonPath("$.code") { value("TOSS_IP_NOT_ALLOWED") }
        }

        willThrow(TossAuthFailedException("401")).given(tossAccountListQueryService).listAccounts()
        mockMvc.get("/api/toss/accounts").andExpect {
            status { isServiceUnavailable() }
            jsonPath("$.code") { value("TOSS_AUTH_FAILED") }
        }

        willThrow(TossIntegrationDisabledException(listOf("TOSS_CLIENT_ID")))
            .given(tossAccountListQueryService)
            .listAccounts()
        mockMvc.get("/api/toss/accounts").andExpect {
            status { isInternalServerError() }
            jsonPath("$.code") { value("TOSS_INTEGRATION_DISABLED") }
        }
    }
}
