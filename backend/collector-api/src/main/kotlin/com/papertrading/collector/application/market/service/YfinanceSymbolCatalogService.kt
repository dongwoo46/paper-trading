package com.papertrading.collector.application.market.service

import com.papertrading.collector.domain.market.YfinanceSymbolCatalog
import com.papertrading.collector.infra.market.persistence.YfinanceSymbolCatalogRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class YfinanceSymbolCatalogService(
	private val repository: YfinanceSymbolCatalogRepository,
) {
	fun search(
		query: String,
		market: String,
		status: String,
		limit: Int,
	): List<YfinanceSymbolCatalog> {
		val safeLimit = limit.coerceIn(1, 500)
		val normalizedStatus = normalizeStatus(status)
		return repository.search(
			query = query.trim(),
			market = market.trim(),
			status = normalizedStatus,
			pageable = PageRequest.of(0, safeLimit),
		)
	}

	fun selected(): List<YfinanceSymbolCatalog> {
		return repository.findByEnabledTrueOrderByTicker()
	}

	fun countAll(): Long = repository.count()

	fun countSubscribed(): Long = repository.countByEnabledTrue()

	@Transactional
	fun add(ticker: String): String {
		val normalized = ticker.trim().uppercase()
		if (normalized.isBlank()) return "invalid_input"
		if (!repository.existsByTicker(normalized)) return "not_in_catalog"
		val changed = repository.enableByTicker(normalized)
		return if (changed > 0) "added" else "already_added"
	}

	@Transactional
	fun remove(ticker: String): String {
		val normalized = ticker.trim().uppercase()
		if (normalized.isBlank()) return "invalid_input"
		if (!repository.existsByTicker(normalized)) return "not_in_catalog"
		val changed = repository.disableByTicker(normalized)
		return if (changed > 0) "removed" else "already_removed"
	}

	@Transactional
	fun updateCollectionStatus(ticker: String, fetchedUntilDate: LocalDate): String {
		val normalized = ticker.trim().uppercase()
		if (normalized.isBlank()) return "invalid_input"
		if (!repository.existsByTicker(normalized)) return "not_in_catalog"
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
