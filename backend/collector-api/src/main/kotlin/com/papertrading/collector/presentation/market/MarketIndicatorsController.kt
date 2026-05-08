package com.papertrading.collector.presentation.market

import com.papertrading.collector.application.market.service.MarketIndicatorsQuery
import com.papertrading.collector.application.market.service.MarketIndicatorsQueryService
import com.papertrading.collector.presentation.market.dto.MarketIndicatorsResponse
import com.papertrading.collector.presentation.market.dto.toResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

@RestController
@RequestMapping("/api/market/indicators")
class MarketIndicatorsController(
	private val marketIndicatorsQueryService: MarketIndicatorsQueryService,
) {
	@GetMapping("/{symbol}")
	fun indicators(
		@PathVariable symbol: String,
		@RequestParam interval: String,
		@RequestParam(required = false) limit: Int?,
		@RequestParam(required = false) from: String?,
		@RequestParam(required = false) to: String?,
		@RequestParam indicators: String,
		@RequestParam(required = false) bbPeriod: Int?,
		@RequestParam(required = false) bbStdDev: BigDecimal?,
		@RequestParam(required = false) rsiPeriod: Int?,
		@RequestParam(required = false) macdFast: Int?,
		@RequestParam(required = false) macdSlow: Int?,
		@RequestParam(required = false) macdSignal: Int?,
	): ResponseEntity<MarketIndicatorsResponse> {
		val result = marketIndicatorsQueryService.query(
			MarketIndicatorsQuery(
				symbol = symbol,
				interval = interval,
				limit = limit,
				from = from?.let { parseTime(interval, it) },
				to = to?.let { parseTime(interval, it) },
				indicators = indicators,
				bbPeriod = bbPeriod,
				bbStdDev = bbStdDev,
				rsiPeriod = rsiPeriod,
				macdFast = macdFast,
				macdSlow = macdSlow,
				macdSignal = macdSignal,
			),
		)
		return ResponseEntity.ok(result.toResponse())
	}

	private fun parseTime(interval: String, value: String): Instant = try {
		Instant.parse(value)
	} catch (_: DateTimeParseException) {
		if (interval == "1d" || interval == "1w") {
			try {
				LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC)
			} catch (_: DateTimeParseException) {
				throw IllegalArgumentException("INVALID_PERIOD_QUERY")
			}
		} else {
			throw IllegalArgumentException("INVALID_PERIOD_QUERY")
		}
	}
}
