package com.papertrading.api

import com.papertrading.api.infrastructure.toss.TossApiClient
import com.papertrading.api.infrastructure.toss.TossProperties
import com.papertrading.api.infrastructure.toss.TossRateLimiter
import com.papertrading.api.infrastructure.toss.TossTokenIssuer
import com.papertrading.api.infrastructure.toss.TossTokenIssuerImpl
import com.papertrading.api.infrastructure.toss.TossTokenManager
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
}
