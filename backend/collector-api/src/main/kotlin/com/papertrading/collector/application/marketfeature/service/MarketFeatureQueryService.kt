package com.papertrading.collector.application.marketfeature.service

import com.papertrading.collector.application.marketfeature.port.MarketFeatureStore
import com.papertrading.collector.domain.marketfeature.FeatureSnapshot
import com.papertrading.collector.domain.marketfeature.FeatureWindow
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus

@Service
class MarketFeatureQueryService(
    private val store: MarketFeatureStore,
) {
    fun getSnapshots(symbol: String, windows: Set<FeatureWindow>): List<FeatureSnapshot> {
        val orderedWindows = FeatureWindow.entries.filter { windows.contains(it) }
        val snapshots = orderedWindows.mapNotNull { window -> store.loadSnapshot(symbol, window) }
        if (snapshots.isEmpty()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "snapshot not found for symbol=$symbol")
        }
        if (snapshots.size != orderedWindows.size) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "aggregation state is inconsistent for symbol=$symbol")
        }
        return snapshots
    }
}

