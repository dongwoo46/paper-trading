package com.papertrading.collector.presentation.market

import com.papertrading.collector.application.market.service.YfinanceSymbolCatalogService
import com.papertrading.collector.presentation.market.dto.YfinanceCollectionStatusRequest
import com.papertrading.collector.presentation.market.dto.YfinanceCollectionStatusResponse
import com.papertrading.collector.presentation.market.dto.YfinanceSelectionChangeResponse
import com.papertrading.collector.presentation.market.dto.YfinanceSelectionRequest
import com.papertrading.collector.presentation.market.dto.YfinanceSymbolResponse
import com.papertrading.collector.presentation.market.dto.toResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/yfinance")
class YfinanceCatalogController(
	private val service: YfinanceSymbolCatalogService,
) {
	@GetMapping("/symbols/search")
	fun search(
		@RequestParam(name = "query", required = false, defaultValue = "") query: String,
		@RequestParam(name = "market", required = false, defaultValue = "") market: String,
		@RequestParam(name = "status", required = false, defaultValue = "all") status: String,
		@RequestParam(name = "limit", required = false, defaultValue = "100") limit: Int,
	): Map<String, Any> {
		val items = service.search(
			query = query,
			market = market,
			status = status,
			limit = limit,
		).map { it.toResponse() }
		return mapOf(
			"items" to items,
			"returnedCount" to items.size,
			"totalCatalogCount" to service.countAll(),
			"totalSubscribedCount" to service.countSubscribed(),
		)
	}

	@GetMapping("/symbols/catalog")
	fun catalog(
		@RequestParam(name = "query", required = false, defaultValue = "") query: String,
		@RequestParam(name = "market", required = false, defaultValue = "") market: String,
		@RequestParam(name = "status", required = false, defaultValue = "all") status: String,
		@RequestParam(name = "limit", required = false, defaultValue = "100") limit: Int,
	): Map<String, Any> {
		val items = service.search(
			query = query,
			market = market,
			status = status,
			limit = limit,
		).map { it.toResponse() }
		return mapOf(
			"items" to items,
			"returnedCount" to items.size,
			"totalCatalogCount" to service.countAll(),
			"totalSubscribedCount" to service.countSubscribed(),
		)
	}

	@GetMapping("/symbols/selections")
	fun selections(): List<YfinanceSymbolResponse> {
		return service.selected().map { it.toResponse() }
	}

	@GetMapping("/symbols/subscriptions")
	fun subscriptions(): List<YfinanceSymbolResponse> {
		return selections()
	}

	@PostMapping("/symbols/selections")
	fun addSelection(@RequestBody request: YfinanceSelectionRequest): ResponseEntity<YfinanceSelectionChangeResponse> {
		val normalized = request.ticker.trim().uppercase()
		val status = service.add(normalized)
		return ResponseEntity.ok(
			YfinanceSelectionChangeResponse(
				status = status,
				ticker = normalized,
				totalSelected = service.selected().size,
			),
		)
	}

	@PostMapping("/symbols/subscriptions")
	fun addSubscription(@RequestBody request: YfinanceSelectionRequest): ResponseEntity<YfinanceSelectionChangeResponse> {
		return addSelection(request)
	}

	@PostMapping("/symbols/collection-status")
	fun updateCollectionStatus(
		@RequestBody request: YfinanceCollectionStatusRequest,
	): ResponseEntity<YfinanceCollectionStatusResponse> {
		val normalized = request.ticker.trim().uppercase()
		val status = service.updateCollectionStatus(
			ticker = normalized,
			fetchedUntilDate = request.fetchedUntilDate,
		)
		return ResponseEntity.ok(
			YfinanceCollectionStatusResponse(
				status = status,
				ticker = normalized,
				fetchedUntilDate = request.fetchedUntilDate,
			),
		)
	}

	@DeleteMapping("/symbols/selections")
	fun removeSelection(@RequestBody request: YfinanceSelectionRequest): ResponseEntity<YfinanceSelectionChangeResponse> {
		val normalized = request.ticker.trim().uppercase()
		val status = service.remove(normalized)
		return ResponseEntity.ok(
			YfinanceSelectionChangeResponse(
				status = status,
				ticker = normalized,
				totalSelected = service.selected().size,
			),
		)
	}

	@DeleteMapping("/symbols/subscriptions")
	fun removeSubscription(@RequestBody request: YfinanceSelectionRequest): ResponseEntity<YfinanceSelectionChangeResponse> {
		return removeSelection(request)
	}
}
