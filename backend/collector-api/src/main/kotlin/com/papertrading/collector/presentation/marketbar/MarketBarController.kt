package com.papertrading.collector.presentation.marketbar

import com.papertrading.collector.application.marketbar.service.MarketBarQueryService
import com.papertrading.collector.domain.marketbar.MarketBar
import com.papertrading.collector.presentation.marketbar.dto.BarDto
import com.papertrading.collector.presentation.marketbar.dto.MarketBarResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/market/bars")
class MarketBarController(
    private val queryService: MarketBarQueryService,
) {
    companion object {
        private const val MAX_LIMIT = 100
    }

    @GetMapping("/{symbol}")
    fun getBars(
        @PathVariable symbol: String,
        @RequestParam(defaultValue = "1m") interval: String,
        @RequestParam(defaultValue = "60") limit: Int,
    ): ResponseEntity<MarketBarResponse> {
        val clampedLimit = minOf(limit, MAX_LIMIT)
        val bars = queryService.getBars(symbol, interval, clampedLimit)
        if (bars.isEmpty()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "no bars found for $symbol/$interval")
        }
        return ResponseEntity.ok(
            MarketBarResponse(
                symbol = symbol,
                interval = interval,
                bars = bars.map { it.toBarDto() },
            ),
        )
    }

    private fun MarketBar.toBarDto(): BarDto = BarDto(
        startedAt = startedAt.toString(),
        open = open.toPlainString(),
        high = high.toPlainString(),
        low = low.toPlainString(),
        close = close.toPlainString(),
        volume = volume.toPlainString(),
        tradeValue = tradeValue.toPlainString(),
        vwap = vwap.toPlainString(),
        tickCount = tickCount,
    )
}
