package com.papertrading.collector.presentation.upbit

import com.papertrading.collector.application.upbit.service.UpbitMarketCatalogService
import com.papertrading.collector.presentation.upbit.dto.UpbitMarketResponse
import com.papertrading.collector.presentation.upbit.dto.UpbitMarketSelectionChangeResponse
import com.papertrading.collector.presentation.upbit.dto.UpbitMarketSelectionRequest
import com.papertrading.collector.presentation.upbit.dto.toResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/upbit/markets")
class UpbitMarketController(
	private val service: UpbitMarketCatalogService,
) {
	@GetMapping("/catalog")
	fun catalog(
		@RequestParam(name = "query", required = false, defaultValue = "") query: String,
		@RequestParam(name = "marketGroup", required = false, defaultValue = "") marketGroup: String,
		@RequestParam(name = "status", required = false, defaultValue = "all") status: String,
		@RequestParam(name = "limit", required = false, defaultValue = "100") limit: Int,
	): Map<String, Any> {
		val items = service.search(
			query = query,
			marketGroup = marketGroup,
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

	@GetMapping("/search")
	fun search(
		@RequestParam(name = "query", required = false, defaultValue = "") query: String,
		@RequestParam(name = "marketGroup", required = false, defaultValue = "") marketGroup: String,
		@RequestParam(name = "status", required = false, defaultValue = "all") status: String,
		@RequestParam(name = "limit", required = false, defaultValue = "100") limit: Int,
	): Map<String, Any> {
		val items = service.search(
			query = query,
			marketGroup = marketGroup,
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

	@GetMapping("/selections")
	fun selections(): List<UpbitMarketResponse> {
		return service.selected().map { it.toResponse() }
	}

	@GetMapping("/subscriptions")
	fun subscriptions(): List<UpbitMarketResponse> {
		return selections()
	}

	@PostMapping("/selections")
	fun addSelection(
		@RequestBody request: UpbitMarketSelectionRequest,
	): ResponseEntity<UpbitMarketSelectionChangeResponse> {
		val normalized = request.market.trim().uppercase()
		val status = service.add(normalized)
		return ResponseEntity.ok(
			UpbitMarketSelectionChangeResponse(
				status = status,
				market = normalized,
				totalSelected = service.selected().size,
			),
		)
	}

	@PostMapping("/subscriptions")
	fun addSubscription(
		@RequestBody request: UpbitMarketSelectionRequest,
	): ResponseEntity<UpbitMarketSelectionChangeResponse> {
		return addSelection(request)
	}

	@PostMapping("/catalog/sync")
	fun syncCatalog(): ResponseEntity<Map<String, Any>> {
		val processed = service.syncCatalogFromUpbit()
		return ResponseEntity.ok(
			mapOf(
				"status" to "synced",
				"processed" to processed,
			),
		)
	}

	@DeleteMapping("/selections")
	fun removeSelection(
		@RequestBody request: UpbitMarketSelectionRequest,
	): ResponseEntity<UpbitMarketSelectionChangeResponse> {
		val normalized = request.market.trim().uppercase()
		val status = service.remove(normalized)
		return ResponseEntity.ok(
			UpbitMarketSelectionChangeResponse(
				status = status,
				market = normalized,
				totalSelected = service.selected().size,
			),
		)
	}

	@DeleteMapping("/subscriptions")
	fun removeSubscription(
		@RequestBody request: UpbitMarketSelectionRequest,
	): ResponseEntity<UpbitMarketSelectionChangeResponse> {
		return removeSelection(request)
	}
}
