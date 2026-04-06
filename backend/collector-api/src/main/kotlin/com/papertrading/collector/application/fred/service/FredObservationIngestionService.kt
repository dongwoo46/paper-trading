package com.papertrading.collector.application.fred.service

import com.fasterxml.jackson.databind.JsonNode
import com.papertrading.collector.infra.fred.FredProperties
import com.papertrading.collector.infra.fred.source.FredClient
import mu.KotlinLogging
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.min

@Service
class FredObservationIngestionService(
	private val fredClient: FredClient,
	private val fredProperties: FredProperties,
	private val fredSeriesCatalogService: FredSeriesCatalogService,
	private val jdbcTemplate: JdbcTemplate,
) {
	private val log = KotlinLogging.logger {}

	private val upsertSql = """
		INSERT INTO fred_series_observation (series_id, observation_date, raw_value, numeric_value, fetched_at, updated_at)
		VALUES (?, ?, ?, ?, ?, NOW())
		ON CONFLICT (series_id, observation_date) DO UPDATE SET
		    raw_value = EXCLUDED.raw_value,
		    numeric_value = EXCLUDED.numeric_value,
		    fetched_at = EXCLUDED.fetched_at,
		    updated_at = NOW()
	""".trimIndent()

	fun collectSelectedSeries(): Map<String, Any> {
		if (!fredProperties.enabled || fredProperties.apiKey.isBlank()) {
			return mapOf(
				"status" to "skipped",
				"reason" to "fred_disabled_or_api_key_missing",
			)
		}

		val seriesIds = fredSeriesCatalogService.selected().map { it.seriesId }
		if (seriesIds.isEmpty()) {
			return mapOf(
				"status" to "skipped",
				"reason" to "no_selected_series",
				"processedSeries" to 0,
				"upsertedObservations" to 0,
			)
		}

		val safeLimit = fredProperties.pollingLimitPerSeries.coerceIn(1, 10_000)
		val requestIntervalMillis = fredProperties.pollingRequestIntervalMillis.coerceAtLeast(0)
		val fetchedAt = LocalDateTime.now()
		var processedSeries = 0
		var upsertedObservations = 0

		seriesIds.forEach { seriesId ->
			runCatching {
				val latestStoredDate = findLatestStoredDate(seriesId)
				val observationStart = latestStoredDate?.plusDays(1)
				val response = fetchObservationsWithRetry(seriesId, safeLimit, observationStart)
				val count = upsertObservations(seriesId, response, fetchedAt)
				processedSeries += 1
				upsertedObservations += count
			}.onFailure { ex ->
				log.warn(ex) { "fred observation collect failed: seriesId=$seriesId" }
			}
			if (requestIntervalMillis > 0) {
				Thread.sleep(requestIntervalMillis)
			}
		}

		return mapOf(
			"status" to "completed",
			"processedSeries" to processedSeries,
			"upsertedObservations" to upsertedObservations,
		)
	}

	private fun upsertObservations(
		seriesId: String,
		response: JsonNode?,
		fetchedAt: LocalDateTime,
	): Int {
		if (response == null) return 0
		val observations = response.path("observations")
		if (!observations.isArray || observations.isEmpty) return 0

		var affected = 0
		observations.forEach { row ->
			val dateText = row.path("date").asText("").trim()
			if (dateText.isBlank()) return@forEach

			val observationDate = runCatching { LocalDate.parse(dateText) }.getOrNull() ?: return@forEach
			val rawValue = row.path("value").asText(".").trim().ifBlank { "." }
			val numericValue = rawValue.toBigDecimalOrNull()

			jdbcTemplate.update(
				upsertSql,
				seriesId,
				observationDate,
				rawValue,
				numericValue,
				fetchedAt,
			)
			affected += 1
		}
		return affected
	}

	private fun fetchObservationsWithRetry(
		seriesId: String,
		limit: Int,
		observationStart: LocalDate?,
	): JsonNode? {
		val maxRetries = fredProperties.pollingMaxRetries.coerceIn(0, 10)
		var attempt = 0
		while (true) {
			try {
				return fredClient.getSeriesObservations(seriesId, limit, observationStart).block()
			} catch (ex: WebClientResponseException.TooManyRequests) {
				if (attempt >= maxRetries) {
					log.warn { "fred observation 429 max retry reached: seriesId=$seriesId, attempt=$attempt" }
					return null
				}
				val backoff = resolveRetryDelayMillis(ex, attempt)
				log.warn { "fred observation 429 received: seriesId=$seriesId, retryInMs=$backoff, attempt=$attempt" }
				Thread.sleep(backoff)
				attempt += 1
			}
		}
	}

	private fun findLatestStoredDate(seriesId: String): LocalDate? {
		val sql = """
			SELECT max(observation_date)
			FROM fred_series_observation
			WHERE series_id = ?
		""".trimIndent()
		return jdbcTemplate.query(sql, { rs, _ ->
			rs.getDate(1)?.toLocalDate()
		}, seriesId).firstOrNull()
	}

	private fun resolveRetryDelayMillis(
		ex: WebClientResponseException.TooManyRequests,
		attempt: Int,
	): Long {
		val retryAfterHeader = ex.headers.getFirst("Retry-After")
		val retryAfterSeconds = retryAfterHeader?.toLongOrNull()
		if (retryAfterSeconds != null && retryAfterSeconds > 0) {
			return retryAfterSeconds * 1000
		}
		val cappedAttempt = min(attempt, 6)
		return 1000L * (1L shl cappedAttempt)
	}

	private fun String.toBigDecimalOrNull(): BigDecimal? {
		if (this == "." || this.equals("nan", ignoreCase = true)) return null
		return runCatching { BigDecimal(this) }.getOrNull()
	}
}
