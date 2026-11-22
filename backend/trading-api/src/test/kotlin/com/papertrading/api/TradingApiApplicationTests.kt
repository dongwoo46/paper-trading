package com.papertrading.api

import com.papertrading.api.application.account.BrokerAccountQueryService
import com.papertrading.api.application.account.port.BrokerAccountBalancePort
import com.papertrading.api.application.toss.TossAccountListQueryService
import com.papertrading.api.application.toss.port.TossAccountListPort
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.infrastructure.kis.KisAccountBalanceAdapter
import com.papertrading.api.infrastructure.toss.TossAccountBalanceAdapter
import com.papertrading.api.infrastructure.toss.TossAccountListClient
import com.papertrading.api.infrastructure.toss.TossApiClient
import com.papertrading.api.infrastructure.toss.TossProperties
import com.papertrading.api.infrastructure.toss.TossRateLimiter
import com.papertrading.api.infrastructure.toss.TossTokenIssuer
import com.papertrading.api.infrastructure.toss.TossTokenIssuerImpl
import com.papertrading.api.infrastructure.toss.TossTokenManager
import com.papertrading.api.presentation.controller.BrokerAccountBalanceController
import com.papertrading.api.presentation.controller.TossAccountController
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class TradingApiApplicationTests(@Autowired private val context: ApplicationContext) {

    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @Container
        @ServiceConnection(name = "redis")
        val redis = GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379)
    }

    @Test
    fun contextLoads() {
    }

    // EDGE-01, FLAG-14: 토스 환경변수 없이 기동해도 토스 전송 계층 빈이 등록되고 토스만 비활성
    @Test
    fun `토스 환경변수가 없어도 토스 전송 계층 빈이 등록되고 토스만 비활성이다`() {
        assertNotNull(context.getBean(TossTokenManager::class.java))
        assertNotNull(context.getBean(TossApiClient::class.java))
        assertNotNull(context.getBean(TossRateLimiter::class.java))
        assertTrue(context.getBean(TossTokenIssuer::class.java) is TossTokenIssuerImpl)
        assertFalse(context.getBean(TossProperties::class.java).integrationEnabled())
    }

    // step 6: 잔고 포트·어댑터·유스케이스·컨트롤러 빈이 토스 비활성에서도 등록됨, 토스 어댑터가 공통 잔고 포트를 구현
    // devkit:test-change D25 step 10 — KIS 어댑터가 같은 포트를 구현해 구현이 둘이 됨, 빈 하나를 기대하던 단언을 모드 표로 대체
    // 이 컨텍스트 기동이 모드 표 검사(RISK-17)의 실행 증거
    @Test
    fun `토스 잔고와 계좌 목록 빈이 등록되고 공통 잔고 포트 구현은 토스와 KIS 어댑터이다`() {
        val portsByMode = context.getBeansOfType(BrokerAccountBalancePort::class.java).values
            .flatMap { port -> port.supportedModes.map { it to port } }
            .toMap()
        assertTrue(portsByMode.getValue(TradingMode.TOSS_LIVE) is TossAccountBalanceAdapter)
        assertTrue(portsByMode.getValue(TradingMode.KIS_PAPER) is KisAccountBalanceAdapter)
        assertTrue(portsByMode.getValue(TradingMode.KIS_LIVE) is KisAccountBalanceAdapter)
        assertTrue(context.getBean(TossAccountListPort::class.java) is TossAccountListClient)
        assertNotNull(context.getBean(BrokerAccountQueryService::class.java))
        assertNotNull(context.getBean(TossAccountListQueryService::class.java))
        assertNotNull(context.getBean(BrokerAccountBalanceController::class.java))
        assertNotNull(context.getBean(TossAccountController::class.java))
    }
}
