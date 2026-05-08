package com.papertrading.collector.presentation.market

import com.papertrading.collector.application.market.service.MarketWeeklyOhlcvQueryService
import com.papertrading.collector.presentation.market.dto.MarketWeeklyBarResponse
import com.papertrading.collector.presentation.market.dto.toResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/market/weekly")
class MarketWeeklyOhlcvController(
	private val queryService: MarketWeeklyOhlcvQueryService,
) {
	@GetMapping("/{symbol}")
	fun weeklyBars(
		@PathVariable symbol: String,
		@RequestParam(name = "source", required = false, defaultValue = "yfinance") source: String,
		@RequestParam(name = "from", required = false) from: LocalDate?,
		@RequestParam(name = "to", required = false) to: LocalDate?,
		@RequestParam(name = "limit", required = false, defaultValue = "260") limit: Int,
	): List<MarketWeeklyBarResponse> {
		return queryService.weeklyBars(
			source = source,
			symbol = symbol,
			from = from,
			to = to,
			limit = limit,
		).map { it.toResponse() }
	}
}
