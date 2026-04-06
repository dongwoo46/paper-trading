package com.papertrading.collector.application.fred.service

import com.papertrading.collector.domain.fred.FredSeriesCatalog
import com.papertrading.collector.infra.fred.source.FredClient
import com.papertrading.collector.infra.fred.persistence.FredSeriesCatalogRepository
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.ArrayDeque

@Service
class FredSeriesCatalogService(
	private val repository: FredSeriesCatalogRepository,
	private val fredClient: FredClient,
	private val jdbcTemplate: JdbcTemplate,
) {
	fun search(
		query: String,
		category: String,
		frequency: String,
		status: String,
		limit: Int,
	): List<FredSeriesCatalog> {
		val safeLimit = limit.coerceIn(1, 500)
		val normalizedStatus = normalizeStatus(status)
		return repository.search(
			query = query.trim(),
			category = category.trim(),
			frequency = frequency.trim(),
			status = normalizedStatus,
			pageable = PageRequest.of(0, safeLimit),
		)
	}

	fun selected(): List<FredSeriesCatalog> {
		return repository.findByEnabledTrueOrderBySeriesId()
	}

	fun getBySeriesId(seriesId: String): FredSeriesCatalog? {
		return repository.findBySeriesId(seriesId.trim().uppercase())
	}

	fun countAll(): Long = repository.count()

	fun countSubscribed(): Long = repository.countByEnabledTrue()

	@Transactional
	fun add(seriesId: String): String {
		val normalizedSeriesId = seriesId.trim().uppercase()
		if (normalizedSeriesId.isBlank()) return "invalid_input"
		if (!repository.existsBySeriesId(normalizedSeriesId)) return "not_in_catalog"
		val changed = repository.enableBySeriesId(normalizedSeriesId)
		return if (changed > 0) "added" else "already_added"
	}

	@Transactional
	fun remove(seriesId: String): String {
		val normalizedSeriesId = seriesId.trim().uppercase()
		if (normalizedSeriesId.isBlank()) return "invalid_input"
		if (!repository.existsBySeriesId(normalizedSeriesId)) return "not_in_catalog"
		val changed = repository.disableBySeriesId(normalizedSeriesId)
		return if (changed > 0) "removed" else "already_removed"
	}

	@Transactional
	fun syncAllCatalog(
		maxCategories: Int,
		pageSize: Int,
	): Map<String, Int> {
		val safeMaxCategories = maxCategories.coerceIn(1, 200_000)
		val safePageSize = pageSize.coerceIn(10, 1000)
		val visited = mutableSetOf<Long>()
		val queue = ArrayDeque<Long>()
		queue.add(0L)

		val upsertSql = """
			INSERT INTO fred_series_catalog (series_id, title, category, frequency, units, enabled, is_default)
			VALUES (?, ?, ?, ?, ?, FALSE, FALSE)
			ON CONFLICT (series_id) DO UPDATE SET
			    title = EXCLUDED.title,
			    category = EXCLUDED.category,
			    frequency = EXCLUDED.frequency,
			    units = EXCLUDED.units,
			    updated_at = CURRENT_TIMESTAMP
		""".trimIndent()

		var processedCategories = 0
		var upsertedSeries = 0

		while (queue.isNotEmpty() && processedCategories < safeMaxCategories) {
			val categoryId = queue.removeFirst()
			if (!visited.add(categoryId)) continue
			processedCategories += 1

			val children = withRetry { fredClient.getCategoryChildren(categoryId).block() }
			children?.path("categories")?.forEach { child ->
				val childId = child.path("id").asLong(-1L)
				if (childId >= 0 && !visited.contains(childId)) {
					queue.addLast(childId)
				}
			}

			var offset = 0
			while (true) {
				val page = withRetry { fredClient.getCategorySeries(categoryId, offset, safePageSize).block() } ?: break
				val total = page.path("count").asInt(0)
				val seriesRows = page.path("seriess")
				if (!seriesRows.isArray || seriesRows.isEmpty) break

				seriesRows.forEach { row ->
					val seriesId = row.path("id").asText("").trim().uppercase()
					if (seriesId.isBlank()) return@forEach
					val title = row.path("title").asText(seriesId).trim().ifBlank { seriesId }
					val frequency = row.path("frequency_short").asText("").trim().ifBlank {
						row.path("frequency").asText("").trim()
					}.ifBlank { "unknown" }
					val units = row.path("units_short").asText("").trim().ifBlank {
						row.path("units").asText("").trim()
					}.ifBlank { "unknown" }
					val category = "cat:$categoryId"

					jdbcTemplate.update(upsertSql, seriesId, title, category, frequency, units)
					upsertedSeries += 1
				}

				offset += seriesRows.size()
				if (offset >= total) break
			}
		}

		return mapOf(
			"processedCategories" to processedCategories,
			"upsertedSeries" to upsertedSeries,
		)
	}

	private fun <T> withRetry(block: () -> T): T {
		var attempt = 0
		var delayMillis = 1000L
		var lastError: Exception? = null
		while (attempt < 8) {
			try {
				return block()
			} catch (ex: WebClientResponseException.TooManyRequests) {
				lastError = ex
				Thread.sleep(delayMillis)
				delayMillis = (delayMillis * 2).coerceAtMost(30_000L)
				attempt += 1
			}
		}
		throw lastError ?: IllegalStateException("FRED retry failed")
	}

	private fun normalizeStatus(status: String): String {
		return when (status.trim().lowercase()) {
			"subscribed", "enabled", "applied" -> "subscribed"
			"unsubscribed", "disabled", "unapplied" -> "unsubscribed"
			else -> "all"
		}
	}
}
