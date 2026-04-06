package com.papertrading.collector.presentation.kis

import com.papertrading.collector.application.kis.service.KisRestWatchlistService
import com.papertrading.collector.application.kis.service.KisWsSubscriptionService
import com.papertrading.collector.domain.kis.SubscriptionChangeStatus
import com.papertrading.collector.infra.kis.KisProperties
import com.papertrading.collector.infra.kis.persistence.KrSymbolRepository
import com.papertrading.collector.presentation.kis.dto.KisSymbolSubscriptionChangeResponse
import com.papertrading.collector.presentation.kis.dto.KisSymbolSubscriptionRequest
import com.papertrading.collector.presentation.kis.dto.KrSymbolResponse
import com.papertrading.collector.presentation.kis.dto.toResponse
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/kis/symbols")
class KisSymbolCatalogController(
	private val krSymbolRepository: KrSymbolRepository,
	private val wsSubscriptionService: KisWsSubscriptionService,
	private val restWatchlistService: KisRestWatchlistService,
	private val kisProperties: KisProperties,
) {
	@GetMapping("/catalog")
	fun catalog(
		@RequestParam(name = "query", required = false, defaultValue = "") query: String,
		@RequestParam(name = "market", required = false, defaultValue = "") market: String,
		@RequestParam(name = "mode", required = false, defaultValue = "paper") mode: String,
		@RequestParam(name = "channel", required = false, defaultValue = "ws") channel: String,
		@RequestParam(name = "status", required = false, defaultValue = "all") status: String,
		@RequestParam(name = "limit", required = false, defaultValue = "100") limit: Int,
	): Map<String, Any> {
		val safeLimit = limit.coerceIn(1, 500)
		val normalizedMode = mode.trim().lowercase()
		val normalizedChannel = channel.trim().lowercase()
		require(normalizedMode in kisProperties.normalizedModes()) { "invalid mode" }
		require(normalizedChannel == "ws" || normalizedChannel == "rest") { "invalid channel" }
		val subscribedSymbols = if (normalizedChannel == "rest") {
			restWatchlistService.listSymbols(normalizedMode)
		} else {
			wsSubscriptionService.listSymbols(normalizedMode)
		}.toSet()

		val items = krSymbolRepository.search(
			query = query.trim(),
			market = market.trim().uppercase(),
			pageable = PageRequest.of(0, safeLimit),
		).map { it.toResponse() }
			.filter { row ->
				when (status.trim().lowercase()) {
					"subscribed", "enabled", "applied" -> subscribedSymbols.contains(row.symbol)
					"unsubscribed", "disabled", "unapplied" -> !subscribedSymbols.contains(row.symbol)
					else -> true
				}
			}

		return mapOf(
			"items" to items,
			"returnedCount" to items.size,
			"totalCatalogCount" to krSymbolRepository.count(),
			"totalSubscribedCount" to subscribedSymbols.size,
			"mode" to normalizedMode,
			"channel" to normalizedChannel,
		)
	}

	@GetMapping("/subscriptions")
	fun subscriptions(
		@RequestParam mode: String,
		@RequestParam(name = "channel", required = false, defaultValue = "ws") channel: String,
	): Map<String, Any> {
		val normalizedMode = mode.lowercase()
		val normalizedChannel = channel.lowercase()
		require(normalizedMode in kisProperties.normalizedModes()) { "invalid mode" }
		val items = if (normalizedChannel == "rest") {
			restWatchlistService.listSymbols(normalizedMode)
		} else {
			wsSubscriptionService.listSymbols(normalizedMode)
		}
		return mapOf(
			"items" to items,
			"returnedCount" to items.size,
			"totalCatalogCount" to krSymbolRepository.count(),
			"totalSubscribedCount" to items.size,
			"mode" to normalizedMode,
			"channel" to normalizedChannel,
		)
	}

	@PostMapping("/subscriptions")
	fun addSubscription(
		@RequestBody request: KisSymbolSubscriptionRequest,
	): ResponseEntity<KisSymbolSubscriptionChangeResponse> {
		val mode = request.mode.trim().lowercase()
		val symbol = request.symbol.trim()
		val channel = request.channel.trim().lowercase()
		if (mode !in kisProperties.normalizedModes() || symbol.isBlank() || (channel != "ws" && channel != "rest")) {
			return ResponseEntity.ok(
				KisSymbolSubscriptionChangeResponse(
					status = "invalid_input",
					mode = mode,
					symbol = symbol,
					channel = channel,
					totalSelected = 0,
				),
			)
		}

		val status = when (if (channel == "rest") restWatchlistService.addSymbol(mode, symbol) else wsSubscriptionService.addSymbol(mode, symbol)) {
			SubscriptionChangeStatus.ADDED -> "added"
			SubscriptionChangeStatus.ALREADY_EXISTS -> "already_added"
			SubscriptionChangeStatus.LIMIT_EXCEEDED -> "limit_exceeded"
			SubscriptionChangeStatus.INVALID_MODE -> "invalid_mode"
			SubscriptionChangeStatus.INVALID_SYMBOL -> "invalid_symbol"
			SubscriptionChangeStatus.REMOVED, SubscriptionChangeStatus.NOT_FOUND -> "added"
		}

		val totalSelected = if (channel == "rest") {
			restWatchlistService.listSymbols(mode).size
		} else {
			wsSubscriptionService.listSymbols(mode).size
		}

		return ResponseEntity.ok(
			KisSymbolSubscriptionChangeResponse(
				status = status,
				mode = mode,
				symbol = symbol,
				channel = channel,
				totalSelected = totalSelected,
			),
		)
	}

	@DeleteMapping("/subscriptions")
	fun removeSubscription(
		@RequestBody request: KisSymbolSubscriptionRequest,
	): ResponseEntity<KisSymbolSubscriptionChangeResponse> {
		val mode = request.mode.trim().lowercase()
		val symbol = request.symbol.trim()
		val channel = request.channel.trim().lowercase()
		if (mode !in kisProperties.normalizedModes() || symbol.isBlank() || (channel != "ws" && channel != "rest")) {
			return ResponseEntity.ok(
				KisSymbolSubscriptionChangeResponse(
					status = "invalid_input",
					mode = mode,
					symbol = symbol,
					channel = channel,
					totalSelected = 0,
				),
			)
		}

		val status = when (if (channel == "rest") restWatchlistService.removeSymbol(mode, symbol) else wsSubscriptionService.removeSymbol(mode, symbol)) {
			SubscriptionChangeStatus.REMOVED -> "removed"
			SubscriptionChangeStatus.NOT_FOUND -> "not_found"
			SubscriptionChangeStatus.INVALID_MODE -> "invalid_mode"
			SubscriptionChangeStatus.INVALID_SYMBOL -> "invalid_symbol"
			SubscriptionChangeStatus.ADDED, SubscriptionChangeStatus.ALREADY_EXISTS, SubscriptionChangeStatus.LIMIT_EXCEEDED -> "removed"
		}

		val totalSelected = if (channel == "rest") {
			restWatchlistService.listSymbols(mode).size
		} else {
			wsSubscriptionService.listSymbols(mode).size
		}

		return ResponseEntity.ok(
			KisSymbolSubscriptionChangeResponse(
				status = status,
				mode = mode,
				symbol = symbol,
				channel = channel,
				totalSelected = totalSelected,
			),
		)
	}
}
