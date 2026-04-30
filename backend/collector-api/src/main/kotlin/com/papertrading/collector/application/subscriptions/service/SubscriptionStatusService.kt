package com.papertrading.collector.application.subscriptions.service

import com.papertrading.collector.application.kis.dto.WsHealthSnapshot
import com.papertrading.collector.application.kis.service.KisRestWatchlistService
import com.papertrading.collector.application.kis.service.KisWsHealthService
import com.papertrading.collector.application.kis.service.KisWsSubscriptionService
import com.papertrading.collector.application.subscriptions.dto.SubscriptionModeStatus
import com.papertrading.collector.application.subscriptions.dto.SubscriptionStatusReport
import com.papertrading.collector.domain.kis.WsConnectionStatus
import com.papertrading.collector.infra.kis.KisProperties
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SubscriptionStatusService(
    private val healthService: KisWsHealthService,
    private val wsSubscriptionService: KisWsSubscriptionService,
    private val restWatchlistService: KisRestWatchlistService,
    private val properties: KisProperties,
) {
    fun report(): SubscriptionStatusReport {
        val maxSlot = properties.maxRealtimeRegistrations
        if (!properties.enabled) {
            return SubscriptionStatusReport(
                generatedAt = Instant.now(),
                modes = emptyList(),
                totalWsSlotUsed = 0,
                totalWsSlotMax = maxSlot,
            )
        }

        val trIdCount = properties.resolvedTrIds().size
        val healthByMode: Map<String, WsHealthSnapshot> = healthService.health().associateBy { it.mode }
        val wsSymbolsByMode: Map<String, List<String>> = wsSubscriptionService.listSymbolsPerMode()

        val modes = properties.normalizedModes().sorted().map { mode ->
            val health = healthByMode[mode]
            val wsSymbols = wsSymbolsByMode[mode].orEmpty().sorted()
            val restSymbols = restWatchlistService.listSymbols(mode).sorted()
            SubscriptionModeStatus(
                mode = mode,
                connectionStatus = health?.status ?: WsConnectionStatus.DISCONNECTED,
                lastConnectedAt = health?.lastConnectedAt,
                reconnectAttempts = health?.reconnectAttempts ?: 0,
                wsSymbols = wsSymbols,
                restSymbols = restSymbols,
                wsSlotUsed = wsSymbols.size * trIdCount,
                wsSlotMax = maxSlot,
            )
        }

        return SubscriptionStatusReport(
            generatedAt = Instant.now(),
            modes = modes,
            totalWsSlotUsed = modes.sumOf { it.wsSlotUsed },
            totalWsSlotMax = maxSlot,
        )
    }
}
