package com.papertrading.collector.application.marketfeature.port

import com.papertrading.collector.domain.marketfeature.FeatureSnapshot
import com.papertrading.collector.domain.marketfeature.FeatureWindow
import com.papertrading.collector.domain.marketfeature.MinuteBarState

interface MarketFeatureStore {
    fun loadCurrent(symbol: String): MinuteBarState?
    fun saveCurrent(symbol: String, state: MinuteBarState)
    fun appendBar(symbol: String, bar: MinuteBarState)
    fun loadRecentBars(symbol: String, limit: Int): List<MinuteBarState>
    fun saveSnapshot(symbol: String, window: FeatureWindow, snapshot: FeatureSnapshot)
    fun loadSnapshot(symbol: String, window: FeatureWindow): FeatureSnapshot? = null
}
