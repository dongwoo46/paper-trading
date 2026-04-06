package com.papertrading.collector.presentation.kis

import com.papertrading.collector.application.kis.service.KisRestWatchlistService
import com.papertrading.collector.domain.kis.SubscriptionChangeStatus
import com.papertrading.collector.infra.kis.KisProperties
import com.papertrading.collector.infra.kis.source.rest.KisQuoteClient
import com.papertrading.collector.presentation.kis.dto.KisSubscriptionChangeResponse
import com.papertrading.collector.presentation.kis.dto.KisSubscriptionRequest
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/kis/rest/watchlist")
class KisRestWatchlistController(
	private val restWatchlistService: KisRestWatchlistService,
	private val kisProperties: KisProperties,
	private val kisQuoteClient: KisQuoteClient,
) {

	@GetMapping
	fun list(): Map<String, List<String>> {
		return kisProperties.normalizedModes()
			.associateWith { mode -> restWatchlistService.listSymbols(mode) }
			.toSortedMap()
	}

	@GetMapping("/price")
	suspend fun price(
		@RequestParam mode: String,
		@RequestParam symbol: String,
	): ResponseEntity<Any> {
		val normalizedMode = mode.lowercase()
		val normalizedSymbol = symbol.trim()
		if (normalizedMode !in kisProperties.normalizedModes() || normalizedSymbol.isBlank()) {
			return ResponseEntity.badRequest().body(
				mapOf(
					"status" to "invalid_input",
					"mode" to normalizedMode,
					"symbol" to normalizedSymbol,
				),
			)
		}

		val response = kisQuoteClient.inquirePrice(normalizedMode, normalizedSymbol).awaitSingle()
		return ResponseEntity.ok(response)
	}

	@PostMapping
	fun add(@RequestBody request: KisSubscriptionRequest): ResponseEntity<KisSubscriptionChangeResponse> {
		val mode = request.mode.lowercase()
		val symbol = request.symbol.trim()
		val status = restWatchlistService.addSymbol(mode, symbol)
		return ResponseEntity.ok(buildResponse(status, mode, symbol))
	}

	@DeleteMapping
	fun remove(@RequestBody request: KisSubscriptionRequest): ResponseEntity<KisSubscriptionChangeResponse> {
		val mode = request.mode.lowercase()
		val symbol = request.symbol.trim()
		val status = restWatchlistService.removeSymbol(mode, symbol)
		return ResponseEntity.ok(buildResponse(status, mode, symbol))
	}

	private fun buildResponse(status: SubscriptionChangeStatus, mode: String, symbol: String): KisSubscriptionChangeResponse {
		val subscriptions = list()
		return KisSubscriptionChangeResponse(
			status = status.name.lowercase(),
			mode = mode,
			symbol = symbol,
			totalRegistrations = subscriptions[mode]?.size ?: 0,
			maxRegistrations = Int.MAX_VALUE,
			subscriptions = subscriptions,
		)
	}
}
