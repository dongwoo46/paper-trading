package com.papertrading.collector.application.marketfeature.service

import com.papertrading.collector.application.marketfeature.port.MarketFeatureStore
import com.papertrading.collector.domain.entity.kis.KisQuoteEvent
import com.papertrading.collector.domain.marketfeature.FeatureSnapshot
import com.papertrading.collector.domain.marketfeature.FeatureWindow
import com.papertrading.collector.domain.marketfeature.MinuteBarState
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.ConcurrentHashMap
import java.time.temporal.ChronoUnit

@Service
class MarketFeatureAggregationService(
    private val store: MarketFeatureStore,
) {
    private val symbolLocks = ConcurrentHashMap<String, Any>()

    fun onTick(event: KisQuoteEvent) {
        val symbol = event.ticker
        synchronized(symbolLocks.computeIfAbsent(symbol) { Any() }) {
            val minute = event.receivedAt.truncatedTo(ChronoUnit.MINUTES).toString()
            val loaded = store.loadCurrent(symbol)
            val current = when {
                loaded == null -> newBar(minute, event)
                loaded.minute != minute -> {
                    store.appendBar(symbol, loaded)
                    newBar(minute, event)
                }
                else -> updateBar(loaded, event)
            }

            store.saveCurrent(symbol, current)
            val bars = store.loadRecentBars(symbol, 10) + current
            saveWindow(symbol, FeatureWindow.M1, bars.takeLast(1))
            saveWindow(symbol, FeatureWindow.M5, bars.takeLast(5))
            saveWindow(symbol, FeatureWindow.M10, bars.takeLast(10))
        }
    }

    private fun saveWindow(symbol: String, window: FeatureWindow, bars: List<MinuteBarState>) {
        if (bars.isEmpty()) return
        val open = bars.first().open
        val high = bars.maxOf { it.high }
        val low = bars.minOf { it.low }
        val close = bars.last().close
        val volume = bars.fold(BigDecimal.ZERO) { acc, bar -> acc + bar.volume }
        val tradeValue = bars.fold(BigDecimal.ZERO) { acc, bar -> acc + bar.tradeValue }
        val buyVolume = bars.fold(BigDecimal.ZERO) { acc, bar -> acc + bar.buyVolume }
        val sellVolume = bars.fold(BigDecimal.ZERO) { acc, bar -> acc + bar.sellVolume }
        val tickCount = bars.sumOf { it.tickCount }
        val returnRate = if (open.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO else {
            close.subtract(open).divide(open, 8, RoundingMode.HALF_UP)
        }
        val vwap = if (volume.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO else {
            tradeValue.divide(volume, 8, RoundingMode.HALF_UP)
        }
        val tradeImbalance = if (volume.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO else {
            buyVolume.subtract(sellVolume).divide(volume, 8, RoundingMode.HALF_UP)
        }

        store.saveSnapshot(
            symbol,
            window,
            FeatureSnapshot(
                window = window,
                open = open,
                high = high,
                low = low,
                close = close,
                returnRate = returnRate,
                volume = volume,
                tradeValue = tradeValue,
                vwap = vwap,
                buyVolume = buyVolume,
                sellVolume = sellVolume,
                tradeImbalance = tradeImbalance,
                tickCount = tickCount,
                startedAt = bars.first().startedAt,
                updatedAt = bars.last().updatedAt,
            ),
        )
    }

    private fun newBar(minute: String, event: KisQuoteEvent): MinuteBarState {
        val tradeValue = event.price.multiply(event.volume)
        return MinuteBarState(
            minute = minute,
            open = event.price,
            high = event.price,
            low = event.price,
            close = event.price,
            volume = event.volume,
            tradeValue = tradeValue,
            buyVolume = BigDecimal.ZERO,
            sellVolume = BigDecimal.ZERO,
            tickCount = 1,
            startedAt = event.receivedAt,
            updatedAt = event.receivedAt,
        )
    }

    private fun updateBar(current: MinuteBarState, event: KisQuoteEvent): MinuteBarState {
        val tradeValue = event.price.multiply(event.volume)
        return current.copy(
            high = current.high.max(event.price),
            low = current.low.min(event.price),
            close = event.price,
            volume = current.volume + event.volume,
            tradeValue = current.tradeValue + tradeValue,
            tickCount = current.tickCount + 1,
            updatedAt = event.receivedAt,
        )
    }
}

