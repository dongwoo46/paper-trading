package com.papertrading.collector.application.upbit.service

import com.papertrading.collector.domain.upbit.UpbitMarketCatalog
import com.papertrading.collector.infra.upbit.persistence.UpbitMarketCatalogRepository
import com.papertrading.collector.infra.upbit.source.rest.UpbitMarketClient
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpbitMarketCatalogService(
	private val repository: UpbitMarketCatalogRepository,
	private val upbitMarketClient: UpbitMarketClient,
	private val jdbcTemplate: JdbcTemplate,
) {
	fun search(
		query: String,
		marketGroup: String,
		status: String,
		limit: Int,
	): List<UpbitMarketCatalog> {
		val safeLimit = limit.coerceIn(1, 500)
		val normalizedStatus = normalizeStatus(status)
		return repository.search(
			query = query.trim(),
			marketGroup = marketGroup.trim(),
			status = normalizedStatus,
			pageable = PageRequest.of(0, safeLimit),
		)
	}

	fun selected(): List<UpbitMarketCatalog> {
		return repository.findByEnabledTrueOrderByMarket()
	}

	fun countAll(): Long = repository.count()

	fun countSubscribed(): Long = repository.countByEnabledTrue()

	@Transactional
	fun add(market: String): String {
		val normalized = market.trim().uppercase()
		if (normalized.isBlank()) return "invalid_input"
		if (!repository.existsByMarket(normalized)) return "not_in_catalog"
		val changed = repository.enableByMarket(normalized)
		return if (changed > 0) "added" else "already_added"
	}

	@Transactional
	fun remove(market: String): String {
		val normalized = market.trim().uppercase()
		if (normalized.isBlank()) return "invalid_input"
		if (!repository.existsByMarket(normalized)) return "not_in_catalog"
		val changed = repository.disableByMarket(normalized)
		return if (changed > 0) "removed" else "already_removed"
	}

	@Transactional
	fun syncCatalogFromUpbit(): Int {
		val payload = upbitMarketClient.getAllMarkets().block()
			?: throw IllegalStateException("Empty response from Upbit market API")
		if (!payload.isArray) return 0

		val sql = """
			INSERT INTO upbit_market_catalog (market, name, market_group, enabled, is_default)
			VALUES (?, ?, ?, FALSE, FALSE)
			ON CONFLICT (market) DO UPDATE SET
			    name = EXCLUDED.name,
			    market_group = EXCLUDED.market_group,
			    updated_at = CURRENT_TIMESTAMP
		""".trimIndent()

		var processed = 0
		payload.forEach { row ->
			val market = row.path("market").asText("").trim().uppercase()
			if (market.isBlank()) return@forEach
			val koreanName = row.path("korean_name").asText("").trim()
			val englishName = row.path("english_name").asText("").trim()
			val name = if (koreanName.isNotBlank()) koreanName else englishName.ifBlank { market }
			val marketGroup = market.substringBefore("-", missingDelimiterValue = market).uppercase()
			jdbcTemplate.update(sql, market, name, marketGroup)
			processed += 1
		}
		return processed
	}

	private fun normalizeStatus(status: String): String {
		return when (status.trim().lowercase()) {
			"subscribed", "enabled", "applied" -> "subscribed"
			"unsubscribed", "disabled", "unapplied" -> "unsubscribed"
			else -> "all"
		}
	}
}
