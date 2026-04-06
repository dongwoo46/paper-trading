package com.papertrading.collector.presentation.market

import com.papertrading.collector.application.market.service.MarketDailyOhlcvQueryService
import com.papertrading.collector.presentation.market.dto.MarketDailyBarResponse
import com.papertrading.collector.presentation.market.dto.MarketDailySymbolSummaryResponse
import com.papertrading.collector.presentation.market.dto.toResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api")
class MarketDailyOhlcvController(
	private val queryService: MarketDailyOhlcvQueryService,
) {
	@GetMapping("/pykrx/ohlcv/symbols")
	fun pykrxSymbols(
		@RequestParam(name = "limit", required = false, defaultValue = "500") limit: Int,
	): List<MarketDailySymbolSummaryResponse> {
		return queryService.listSymbols("pykrx", limit).map { it.toResponse() }
	}

	@GetMapping("/yfinance/ohlcv/symbols")
	fun yfinanceSymbols(
		@RequestParam(name = "limit", required = false, defaultValue = "500") limit: Int,
	): List<MarketDailySymbolSummaryResponse> {
		return queryService.listSymbols("yfinance", limit).map { it.toResponse() }
	}

	@GetMapping("/pykrx/ohlcv/daily")
	fun pykrxDaily(
		@RequestParam symbol: String,
		@RequestParam(name = "from", required = false) from: LocalDate?,
		@RequestParam(name = "to", required = false) to: LocalDate?,
	): List<MarketDailyBarResponse> {
		return queryService.dailyBars(
			source = "pykrx",
			symbol = symbol,
			from = from,
			to = to,
		).map { it.toResponse() }
	}

	@GetMapping("/yfinance/ohlcv/daily")
	fun yfinanceDaily(
		@RequestParam symbol: String,
		@RequestParam(name = "from", required = false) from: LocalDate?,
		@RequestParam(name = "to", required = false) to: LocalDate?,
	): List<MarketDailyBarResponse> {
		return queryService.dailyBars(
			source = "yfinance",
			symbol = symbol,
			from = from,
			to = to,
		).map { it.toResponse() }
	}
}

