package com.papertrading.collector.presentation.fred

import com.papertrading.collector.application.fred.service.FredSeriesCatalogService
import com.papertrading.collector.infra.fred.source.FredClient
import com.papertrading.collector.presentation.fred.dto.FredSeriesInfoResponse
import com.papertrading.collector.presentation.fred.dto.FredSeriesResponse
import com.papertrading.collector.presentation.fred.dto.FredSeriesSelectionChangeResponse
import com.papertrading.collector.presentation.fred.dto.FredSeriesSelectionRequest
import com.papertrading.collector.presentation.fred.dto.toResponse
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
@RequestMapping("/api/fred")
class FredController(
	private val fredClient: FredClient,
	private val fredSeriesCatalogService: FredSeriesCatalogService,
) {
	@GetMapping("/series/catalog")
	fun catalog(
		@RequestParam(name = "query", required = false, defaultValue = "") query: String,
		@RequestParam(name = "category", required = false, defaultValue = "") category: String,
		@RequestParam(name = "frequency", required = false, defaultValue = "") frequency: String,
		@RequestParam(name = "status", required = false, defaultValue = "all") status: String,
		@RequestParam(name = "limit", required = false, defaultValue = "100") limit: Int,
	): Map<String, Any> {
		val items = fredSeriesCatalogService.search(
			query = query,
			category = category,
			frequency = frequency,
			status = status,
			limit = limit,
		).map { it.toResponse() }
		return mapOf(
			"items" to items,
			"returnedCount" to items.size,
			"totalCatalogCount" to fredSeriesCatalogService.countAll(),
			"totalSubscribedCount" to fredSeriesCatalogService.countSubscribed(),
		)
	}

	@GetMapping("/series/selections")
	fun selections(): List<FredSeriesResponse> {
		return fredSeriesCatalogService.selected().map { it.toResponse() }
	}

	@GetMapping("/series/subscriptions")
	fun subscriptions(): List<FredSeriesResponse> {
		return selections()
	}

	@PostMapping("/series/selections")
	fun addSelection(
		@RequestBody request: FredSeriesSelectionRequest,
	): ResponseEntity<FredSeriesSelectionChangeResponse> {
		val normalizedSeriesId = request.seriesId.trim().uppercase()
		val status = fredSeriesCatalogService.add(normalizedSeriesId)
		val totalSelected = fredSeriesCatalogService.selected().size
		return ResponseEntity.ok(
			FredSeriesSelectionChangeResponse(
				status = status,
				seriesId = normalizedSeriesId,
				totalSelected = totalSelected,
			),
		)
	}

	@PostMapping("/series/subscriptions")
	fun addSubscription(
		@RequestBody request: FredSeriesSelectionRequest,
	): ResponseEntity<FredSeriesSelectionChangeResponse> {
		return addSelection(request)
	}

	@PostMapping("/series/catalog/sync")
	fun syncCatalog(
		@RequestParam(name = "maxCategories", required = false, defaultValue = "500") maxCategories: Int,
		@RequestParam(name = "pageSize", required = false, defaultValue = "100") pageSize: Int,
	): ResponseEntity<Map<String, Any>> {
		val result = fredSeriesCatalogService.syncAllCatalog(
			maxCategories = maxCategories,
			pageSize = pageSize,
		)
		return ResponseEntity.ok(
			mapOf(
				"status" to "synced",
				"processedCategories" to (result["processedCategories"] ?: 0),
				"upsertedSeries" to (result["upsertedSeries"] ?: 0),
			),
		)
	}

	@DeleteMapping("/series/selections")
	fun removeSelection(
		@RequestBody request: FredSeriesSelectionRequest,
	): ResponseEntity<FredSeriesSelectionChangeResponse> {
		val normalizedSeriesId = request.seriesId.trim().uppercase()
		val status = fredSeriesCatalogService.remove(normalizedSeriesId)
		val totalSelected = fredSeriesCatalogService.selected().size
		return ResponseEntity.ok(
			FredSeriesSelectionChangeResponse(
				status = status,
				seriesId = normalizedSeriesId,
				totalSelected = totalSelected,
			),
		)
	}

	@DeleteMapping("/series/subscriptions")
	fun removeSubscription(
		@RequestBody request: FredSeriesSelectionRequest,
	): ResponseEntity<FredSeriesSelectionChangeResponse> {
		return removeSelection(request)
	}

	@GetMapping("/series/observations")
	suspend fun getSeriesObservations(
		@RequestParam seriesId: String,
		@RequestParam(required = false, defaultValue = "100") limit: Int,
	): ResponseEntity<Any> {
		val normalizedSeriesId = seriesId.trim().uppercase()
		val safeLimit = limit.coerceIn(1, 100000)
		if (normalizedSeriesId.isBlank()) {
			return ResponseEntity.badRequest().body(
				mapOf(
					"status" to "invalid_input",
					"seriesId" to normalizedSeriesId,
				),
			)
		}

		val response = fredClient.getSeriesObservations(normalizedSeriesId, safeLimit).awaitSingle()
		return ResponseEntity.ok(response)
	}

	@GetMapping("/series/search")
	suspend fun searchSeries(
		@RequestParam query: String,
		@RequestParam(required = false, defaultValue = "50") limit: Int,
	): ResponseEntity<Any> {
		val normalizedQuery = query.trim()
		val safeLimit = limit.coerceIn(1, 1000)
		if (normalizedQuery.isBlank()) {
			return ResponseEntity.badRequest().body(
				mapOf(
					"status" to "invalid_input",
					"query" to normalizedQuery,
				),
			)
		}

		val response = fredClient.searchSeries(normalizedQuery, safeLimit).awaitSingle()
		return ResponseEntity.ok(response)
	}

	@GetMapping("/series/info")
	suspend fun seriesInfo(
		@RequestParam seriesId: String,
		@RequestParam(name = "observationLimit", required = false, defaultValue = "30") observationLimit: Int,
	): ResponseEntity<FredSeriesInfoResponse> {
		val normalizedSeriesId = seriesId.trim().uppercase()
		require(normalizedSeriesId.isNotBlank()) { "seriesId is required" }
		val safeLimit = observationLimit.coerceIn(1, 1000)
		val catalog = fredSeriesCatalogService.getBySeriesId(normalizedSeriesId)
		val observations = runCatching {
			fredClient.getSeriesObservations(normalizedSeriesId, safeLimit).awaitSingle()
		}.getOrNull()

		return ResponseEntity.ok(
			FredSeriesInfoResponse(
				seriesId = normalizedSeriesId,
				title = catalog?.title,
				category = catalog?.category,
				frequency = catalog?.frequency,
				units = catalog?.units,
				enabled = catalog?.enabled,
				isDefault = catalog?.isDefault,
				observations = observations,
			),
		)
	}
}
