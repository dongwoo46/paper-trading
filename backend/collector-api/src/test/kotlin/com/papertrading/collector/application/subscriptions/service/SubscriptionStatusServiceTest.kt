package com.papertrading.collector.application.subscriptions.service

import com.papertrading.collector.application.kis.dto.WsHealthSnapshot
import com.papertrading.collector.application.kis.service.KisRestWatchlistService
import com.papertrading.collector.application.kis.service.KisWsHealthService
import com.papertrading.collector.application.kis.service.KisWsSubscriptionService
import com.papertrading.collector.domain.kis.WsConnectionStatus
import com.papertrading.collector.infra.kis.KisProperties
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class SubscriptionStatusServiceTest {

    private lateinit var healthService: KisWsHealthService
    private lateinit var wsSubscriptionService: KisWsSubscriptionService
    private lateinit var restWatchlistService: KisRestWatchlistService
    private lateinit var properties: KisProperties
    private lateinit var service: SubscriptionStatusService

    @BeforeEach
    fun setUp() {
        healthService = mockk()
        wsSubscriptionService = mockk()
        restWatchlistService = mockk()
        properties = mockk()
        service = SubscriptionStatusService(healthService, wsSubscriptionService, restWatchlistService, properties)
    }

    @Test
    fun `report_returns_empty_when_kis_disabled`() {
        every { properties.enabled } returns false
        every { properties.maxRealtimeRegistrations } returns 41

        val report = service.report()

        assertThat(report.modes).isEmpty()
        assertThat(report.totalWsSlotUsed).isEqualTo(0)
        assertThat(report.totalWsSlotMax).isEqualTo(41)
    }

    @Test
    fun `report_assembles_mode_status_correctly`() {
        val now = Instant.now()
        every { properties.enabled } returns true
        every { properties.maxRealtimeRegistrations } returns 41
        every { properties.normalizedModes() } returns listOf("paper")
        every { properties.resolvedTrIds() } returns listOf("H0STCNT0")
        every { healthService.health() } returns listOf(
            WsHealthSnapshot(
                mode = "paper",
                status = WsConnectionStatus.CONNECTED,
                lastConnectedAt = now,
                reconnectAttempts = 0,
            )
        )
        every { wsSubscriptionService.listSymbolsPerMode() } returns mapOf("paper" to listOf("005930", "035720"))
        every { restWatchlistService.listSymbols("paper") } returns listOf("035420")

        val report = service.report()

        assertThat(report.modes).hasSize(1)
        val mode = report.modes[0]
        assertThat(mode.mode).isEqualTo("paper")
        assertThat(mode.connectionStatus).isEqualTo(WsConnectionStatus.CONNECTED)
        assertThat(mode.lastConnectedAt).isEqualTo(now)
        assertThat(mode.wsSymbols).containsExactly("005930", "035720")
        assertThat(mode.restSymbols).containsExactly("035420")
        assertThat(mode.wsSlotUsed).isEqualTo(2)
        assertThat(mode.wsSlotMax).isEqualTo(41)
    }

    @Test
    fun `report_sorts_modes_alphabetically`() {
        every { properties.enabled } returns true
        every { properties.maxRealtimeRegistrations } returns 41
        every { properties.normalizedModes() } returns listOf("paper", "live")
        every { properties.resolvedTrIds() } returns listOf("H0STCNT0")
        every { healthService.health() } returns listOf(
            WsHealthSnapshot("paper", WsConnectionStatus.DISCONNECTED, null, 0),
            WsHealthSnapshot("live", WsConnectionStatus.CONNECTED, null, 0),
        )
        every { wsSubscriptionService.listSymbolsPerMode() } returns mapOf("paper" to emptyList(), "live" to emptyList())
        every { restWatchlistService.listSymbols("paper") } returns emptyList()
        every { restWatchlistService.listSymbols("live") } returns emptyList()

        val report = service.report()

        assertThat(report.modes.map { it.mode }).containsExactly("live", "paper")
    }

    @Test
    fun `report_calculates_wsSlotUsed_with_multiple_trIds`() {
        every { properties.enabled } returns true
        every { properties.maxRealtimeRegistrations } returns 41
        every { properties.normalizedModes() } returns listOf("paper")
        every { properties.resolvedTrIds() } returns listOf("H0STCNT0", "H0STASP0")
        every { healthService.health() } returns listOf(
            WsHealthSnapshot("paper", WsConnectionStatus.CONNECTED, null, 0)
        )
        every { wsSubscriptionService.listSymbolsPerMode() } returns mapOf("paper" to listOf("005930", "035720", "000660"))
        every { restWatchlistService.listSymbols("paper") } returns emptyList()

        val report = service.report()

        assertThat(report.modes[0].wsSlotUsed).isEqualTo(6)
    }

    @Test
    fun `report_sets_totalWsSlotUsed_as_sum_of_all_modes`() {
        every { properties.enabled } returns true
        every { properties.maxRealtimeRegistrations } returns 41
        every { properties.normalizedModes() } returns listOf("paper", "live")
        every { properties.resolvedTrIds() } returns listOf("H0STCNT0")
        every { healthService.health() } returns listOf(
            WsHealthSnapshot("paper", WsConnectionStatus.CONNECTED, null, 0),
            WsHealthSnapshot("live", WsConnectionStatus.CONNECTED, null, 0),
        )
        every { wsSubscriptionService.listSymbolsPerMode() } returns mapOf(
            "paper" to listOf("005930"),
            "live" to listOf("035720", "000660"),
        )
        every { restWatchlistService.listSymbols("paper") } returns emptyList()
        every { restWatchlistService.listSymbols("live") } returns emptyList()

        val report = service.report()

        assertThat(report.totalWsSlotUsed).isEqualTo(3)
    }

    @Test
    fun `report_returns_empty_restSymbols_when_mode_not_in_rest_watchlist`() {
        every { properties.enabled } returns true
        every { properties.maxRealtimeRegistrations } returns 41
        every { properties.normalizedModes() } returns listOf("paper")
        every { properties.resolvedTrIds() } returns listOf("H0STCNT0")
        every { healthService.health() } returns listOf(
            WsHealthSnapshot("paper", WsConnectionStatus.CONNECTED, null, 0)
        )
        every { wsSubscriptionService.listSymbolsPerMode() } returns mapOf("paper" to listOf("005930"))
        // REST watchlist returns empty for "paper" (not registered)
        every { restWatchlistService.listSymbols("paper") } returns emptyList()

        val report = service.report()

        assertThat(report.modes[0].restSymbols).isEmpty()
    }

    @Test
    fun `report_falls_back_to_disconnected_when_health_snapshot_missing`() {
        every { properties.enabled } returns true
        every { properties.maxRealtimeRegistrations } returns 41
        every { properties.normalizedModes() } returns listOf("live")
        every { properties.resolvedTrIds() } returns listOf("H0STCNT0")
        // health returns empty (mode not registered in registry)
        every { healthService.health() } returns emptyList()
        every { wsSubscriptionService.listSymbolsPerMode() } returns mapOf("live" to emptyList())
        every { restWatchlistService.listSymbols("live") } returns emptyList()

        val report = service.report()

        assertThat(report.modes[0].connectionStatus).isEqualTo(WsConnectionStatus.DISCONNECTED)
        assertThat(report.modes[0].reconnectAttempts).isEqualTo(0)
        assertThat(report.modes[0].lastConnectedAt).isNull()
    }

    @Test
    fun `report_returns_wsSymbols_sorted_alphabetically`() {
        every { properties.enabled } returns true
        every { properties.maxRealtimeRegistrations } returns 41
        every { properties.normalizedModes() } returns listOf("paper")
        every { properties.resolvedTrIds() } returns listOf("H0STCNT0")
        every { healthService.health() } returns listOf(
            WsHealthSnapshot("paper", WsConnectionStatus.CONNECTED, null, 0)
        )
        // Redis set returns in arbitrary order
        every { wsSubscriptionService.listSymbolsPerMode() } returns mapOf("paper" to listOf("035720", "000660", "005930"))
        every { restWatchlistService.listSymbols("paper") } returns emptyList()

        val report = service.report()

        assertThat(report.modes[0].wsSymbols).containsExactly("000660", "005930", "035720")
    }
}
