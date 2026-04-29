package com.papertrading.collector.application.kis.service

import com.papertrading.collector.domain.kis.WsConnectionStatus
import com.papertrading.collector.infra.kis.KisProperties
import com.papertrading.collector.infra.kis.source.ws.KisWsConnectionRegistry
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KisWsHealthServiceTest {

    private lateinit var registry: KisWsConnectionRegistry
    private lateinit var properties: KisProperties
    private lateinit var service: KisWsHealthService

    @BeforeEach
    fun setUp() {
        registry = KisWsConnectionRegistry()
        properties = mockk()
        service = KisWsHealthService(registry, properties)
    }

    @Test
    fun `health_returns_snapshots_sorted_by_mode`() {
        every { properties.normalizedModes() } returns listOf("paper", "live")

        registry.markConnected("paper")
        registry.markReconnecting("live", 2)

        val snapshots = service.health()

        assertThat(snapshots).hasSize(2)
        assertThat(snapshots[0].mode).isEqualTo("live")
        assertThat(snapshots[1].mode).isEqualTo("paper")
    }

    @Test
    fun `health_delegates_to_registry_with_normalized_modes`() {
        every { properties.normalizedModes() } returns listOf("paper")

        registry.markConnected("paper")

        val snapshots = service.health()

        assertThat(snapshots).hasSize(1)
        assertThat(snapshots[0].mode).isEqualTo("paper")
        assertThat(snapshots[0].status).isEqualTo(WsConnectionStatus.CONNECTED)
    }
}
