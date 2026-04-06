package com.papertrading.collector.presentation.market

import com.papertrading.collector.application.market.service.PykrxSymbolCatalogService
import com.papertrading.collector.presentation.market.dto.PykrxCollectionStatusRequest
import com.papertrading.collector.presentation.market.dto.PykrxCollectionStatusResponse
import com.papertrading.collector.presentation.market.dto.PykrxSelectionChangeResponse
import com.papertrading.collector.presentation.market.dto.PykrxSelectionRequest
import com.papertrading.collector.presentation.market.dto.PykrxSymbolResponse
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
@RequestMapping("/api/pykrx")
class PykrxCatalogController(
	private val service: PykrxSymbolCatalogService,
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
	fun selections(): List<PykrxSymbolResponse> {
		return service.selected().map { it.toResponse() }
	}

	@GetMapping("/symbols/subscriptions")
	fun subscriptions(): List<PykrxSymbolResponse> {
		return selections()
	}

	@PostMapping("/symbols/selections")
	fun addSelection(@RequestBody request: PykrxSelectionRequest): ResponseEntity<PykrxSelectionChangeResponse> {
		val normalized = request.symbol.trim()
		val status = service.add(normalized)
		return ResponseEntity.ok(
			PykrxSelectionChangeResponse(
				status = status,
				symbol = normalized,
				totalSelected = service.selected().size,
			),
		)
	}

	@PostMapping("/symbols/subscriptions")
	fun addSubscription(@RequestBody request: PykrxSelectionRequest): ResponseEntity<PykrxSelectionChangeResponse> {
		return addSelection(request)
	}

	@PostMapping("/symbols/catalog/sync")
	fun syncCatalog(): ResponseEntity<Map<String, Any>> {
		val affected = service.syncCatalogFromKrSymbol()
		return ResponseEntity.ok(
			mapOf(
				"status" to "synced",
				"affectedRows" to affected,
			),
		)
	}

	@PostMapping("/symbols/collection-status")
	fun updateCollectionStatus(
		@RequestBody request: PykrxCollectionStatusRequest,
	): ResponseEntity<PykrxCollectionStatusResponse> {
		val normalized = request.symbol.trim()
		val status = service.updateCollectionStatus(
			symbol = normalized,
			fetchedUntilDate = request.fetchedUntilDate,
		)
		return ResponseEntity.ok(
			PykrxCollectionStatusResponse(
				status = status,
				symbol = normalized,
				fetchedUntilDate = request.fetchedUntilDate,
			),
		)
	}

	@DeleteMapping("/symbols/selections")
	fun removeSelection(@RequestBody request: PykrxSelectionRequest): ResponseEntity<PykrxSelectionChangeResponse> {
		val normalized = request.symbol.trim()
		val status = service.remove(normalized)
		return ResponseEntity.ok(
			PykrxSelectionChangeResponse(
				status = status,
				symbol = normalized,
				totalSelected = service.selected().size,
			),
		)
	}

	@DeleteMapping("/symbols/subscriptions")
	fun removeSubscription(@RequestBody request: PykrxSelectionRequest): ResponseEntity<PykrxSelectionChangeResponse> {
		return removeSelection(request)
	}
}
