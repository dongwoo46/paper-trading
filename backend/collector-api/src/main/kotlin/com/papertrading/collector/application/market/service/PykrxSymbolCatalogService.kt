package com.papertrading.collector.application.market.service

import com.papertrading.collector.domain.market.PykrxSymbolCatalog
import com.papertrading.collector.infra.market.persistence.PykrxSymbolCatalogRepository
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class PykrxSymbolCatalogService(
	private val repository: PykrxSymbolCatalogRepository,
	private val jdbcTemplate: JdbcTemplate,
) {
	fun search(
		query: String,
		market: String,
		status: String,
		limit: Int,
	): List<PykrxSymbolCatalog> {
		val safeLimit = limit.coerceIn(1, 500)
		val normalizedStatus = normalizeStatus(status)
		return repository.search(
			query = query.trim(),
			market = market.trim(),
			status = normalizedStatus,
			pageable = PageRequest.of(0, safeLimit),
		)
	}

	fun selected(): List<PykrxSymbolCatalog> {
		return repository.findByEnabledTrueOrderBySymbol()
	}

	fun countAll(): Long = repository.count()

	fun countSubscribed(): Long = repository.countByEnabledTrue()

	@Transactional
	fun add(symbol: String): String {
		val normalized = symbol.trim()
		if (normalized.isBlank()) return "invalid_input"
		if (!repository.existsBySymbol(normalized)) return "not_in_catalog"
		val changed = repository.enableBySymbol(normalized)
		return if (changed > 0) "added" else "already_added"
	}

	@Transactional
	fun remove(symbol: String): String {
		val normalized = symbol.trim()
		if (normalized.isBlank()) return "invalid_input"
		if (!repository.existsBySymbol(normalized)) return "not_in_catalog"
		val changed = repository.disableBySymbol(normalized)
		return if (changed > 0) "removed" else "already_removed"
	}

	@Transactional
	fun syncCatalogFromKrSymbol(): Int {
		val sql = """
			INSERT INTO pykrx_symbol_catalog (symbol, name, market, enabled, is_default)
			SELECT k.symbol, k.name, k.market, FALSE, FALSE
			FROM kr_symbol k
			ON CONFLICT (symbol) DO UPDATE SET
			    name = EXCLUDED.name,
			    market = EXCLUDED.market,
			    updated_at = CURRENT_TIMESTAMP
		""".trimIndent()
		return jdbcTemplate.update(sql)
	}

	@Transactional
	fun updateCollectionStatus(symbol: String, fetchedUntilDate: LocalDate): String {
		val normalized = symbol.trim()
		if (normalized.isBlank()) return "invalid_input"
		if (!repository.existsBySymbol(normalized)) return "not_in_catalog"
		val changed = repository.updateCollectionStatus(normalized, fetchedUntilDate)
		return if (changed > 0) "updated" else "not_found"
	}

	private fun normalizeStatus(status: String): String {
		return when (status.trim().lowercase()) {
			"subscribed", "enabled", "applied" -> "subscribed"
			"unsubscribed", "disabled", "unapplied" -> "unsubscribed"
			else -> "all"
		}
	}
}
