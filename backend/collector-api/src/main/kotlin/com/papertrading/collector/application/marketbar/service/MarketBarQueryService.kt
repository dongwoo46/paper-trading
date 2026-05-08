package com.papertrading.collector.application.marketbar.service

import com.papertrading.collector.application.marketbar.port.MarketBarRepository
import com.papertrading.collector.domain.marketbar.MarketBar
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class MarketBarQueryService(
    private val repository: MarketBarRepository,
) {
    companion object {
        private val VALID_INTERVALS = setOf("1m", "5m", "10m")
        private const val MIN_LIMIT = 1
        private const val MAX_LIMIT = 100
    }

    fun getBars(symbol: String, interval: String, limit: Int): List<MarketBar> {
        if (interval !in VALID_INTERVALS) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid interval: $interval")
        }
        if (limit < MIN_LIMIT) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be >= 1")
        }
        val clampedLimit = minOf(limit, MAX_LIMIT)
        return repository.findBars(symbol, interval, clampedLimit)
    }
}
