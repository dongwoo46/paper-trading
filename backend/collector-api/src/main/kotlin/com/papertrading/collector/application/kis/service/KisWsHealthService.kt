package com.papertrading.collector.application.kis.service

import com.papertrading.collector.application.kis.dto.WsHealthSnapshot
import com.papertrading.collector.infra.kis.KisProperties
import com.papertrading.collector.infra.kis.source.ws.KisWsConnectionRegistry
import org.springframework.stereotype.Service

@Service
class KisWsHealthService(
    private val registry: KisWsConnectionRegistry,
    private val properties: KisProperties,
) {
    fun health(): List<WsHealthSnapshot> {
        return registry.snapshot(properties.normalizedModes())
            .sortedBy { it.mode }
    }
}
