package com.papertrading.collector.application.market.service

import com.papertrading.collector.domain.entity.market.MarketWeeklyOhlcv
import com.papertrading.collector.infra.market.persistence.MarketWeeklyOhlcvRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class MarketWeeklyOhlcvQueryService(
	private val repository: MarketWeeklyOhlcvRepository,
) {
	fun weeklyBars(
		source: String,
		symbol: String,
		from: LocalDate?,
		to: LocalDate?,
		limit: Int,
	): List<MarketWeeklyOhlcv> {
		val normalizedSource = source.lowercase()
		val normalizedSymbol = symbol.trim().uppercase()
		if (normalizedSymbol.isBlank()) return emptyList()

		val defaultTo = to ?: LocalDate.now()
		val defaultFrom = from ?: defaultTo.minusYears(1)
		val safeFrom = if (defaultFrom <= defaultTo) defaultFrom else defaultTo
		val safeTo = if (defaultTo >= safeFrom) defaultTo else safeFrom
		val safeLimit = limit.coerceIn(1, 520)

		return repository.findBySourceAndSymbolAndTradeDateBetweenOrderByTradeDateAsc(
			source = normalizedSource,
			symbol = normalizedSymbol,
			from = safeFrom,
			to = safeTo,
		).take(safeLimit)
	}
}
