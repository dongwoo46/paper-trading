package com.papertrading.collector.presentation.market

import com.papertrading.collector.application.market.service.MarketMicrostructureQuery
import com.papertrading.collector.application.market.service.MarketMicrostructureQueryService
import com.papertrading.collector.presentation.market.dto.MarketMicrostructureResponse
import com.papertrading.collector.presentation.market.dto.toResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

@RestController
@RequestMapping("/api/market/microstructure")
class MarketMicrostructureController(
	private val marketMicrostructureQueryService: MarketMicrostructureQueryService,
) {
	@GetMapping("/{symbol}")
	fun getMicrostructure(
		@PathVariable symbol: String,
		@RequestParam interval: String,
		@RequestParam(required = false) session: String?,
		@RequestParam(required = false) limit: Int?,
		@RequestParam(required = false) from: String?,
		@RequestParam(required = false) to: String?,
		@RequestParam(required = false) benchmark: String?,
		@RequestParam(required = false) sector: String?,
	): ResponseEntity<MarketMicrostructureResponse> {
		val result = marketMicrostructureQueryService.query(
			MarketMicrostructureQuery(
				symbol = symbol,
				interval = interval,
				session = session,
				limit = limit,
				from = from?.let { parseTime(interval, it) },
				to = to?.let { parseTime(interval, it) },
				benchmark = benchmark,
				sector = sector,
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
