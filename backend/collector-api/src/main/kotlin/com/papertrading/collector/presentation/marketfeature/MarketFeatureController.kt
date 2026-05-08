package com.papertrading.collector.presentation.marketfeature

import com.papertrading.collector.application.marketfeature.service.MarketFeatureQueryService
import com.papertrading.collector.domain.marketfeature.FeatureWindow
import com.papertrading.collector.presentation.marketfeature.dto.FeatureItemResponse
import com.papertrading.collector.presentation.marketfeature.dto.MarketFeatureResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/market/features")
class MarketFeatureController(
    private val queryService: MarketFeatureQueryService,
) {
    @GetMapping("/{symbol}")
    fun getFeatures(
        @PathVariable symbol: String,
        @RequestParam(required = false, defaultValue = "1m,5m,10m") windows: String,
    ): MarketFeatureResponse {
        validateSymbol(symbol)
        val parsedWindows = parseWindows(windows)
        val snapshots = queryService.getSnapshots(symbol, parsedWindows)
        val asOf = snapshots.maxOf { it.updatedAt }
        return MarketFeatureResponse(
            symbol = symbol,
            asOf = asOf.toString(),
            features = snapshots.map { snapshot ->
                FeatureItemResponse(
                    window = snapshot.window.name.lowercase().replace("m", "") + "m",
                    open = snapshot.open.toPlainString(),
                    high = snapshot.high.toPlainString(),
                    low = snapshot.low.toPlainString(),
                    close = snapshot.close.toPlainString(),
                    returnRate = snapshot.returnRate.toPlainString(),
                    volume = snapshot.volume.toPlainString(),
                    tradeValue = snapshot.tradeValue.toPlainString(),
                    vwap = snapshot.vwap.toPlainString(),
                    buyVolume = snapshot.buyVolume.toPlainString(),
                    sellVolume = snapshot.sellVolume.toPlainString(),
                    tradeImbalance = snapshot.tradeImbalance.toPlainString(),
                    tickCount = snapshot.tickCount,
                    startedAt = snapshot.startedAt.toString(),
                    updatedAt = snapshot.updatedAt.toString(),
                )
            },
        )
    }

    private fun validateSymbol(symbol: String) {
        if (symbol.isBlank() || symbol.contains(' ')) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid symbol")
        }
    }

    private fun parseWindows(raw: String): Set<FeatureWindow> {
        val tokens = raw.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        if (tokens.isEmpty()) {
            return setOf(FeatureWindow.M1, FeatureWindow.M5, FeatureWindow.M10)
        }
        val windows = tokens.map { token ->
            when (token) {
                "1m" -> FeatureWindow.M1
                "5m" -> FeatureWindow.M5
                "10m" -> FeatureWindow.M10
                else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid windows")
            }
        }.toSet()
        if (windows.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid windows")
        }
        return windows
    }
}

