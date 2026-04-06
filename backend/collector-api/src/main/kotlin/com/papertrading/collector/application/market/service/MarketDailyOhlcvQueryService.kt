package com.papertrading.collector.application.market.service

import com.papertrading.collector.domain.market.MarketDailyOhlcv
import com.papertrading.collector.infra.market.persistence.MarketDailyOhlcvRepository
import com.papertrading.collector.infra.market.persistence.MarketDailySymbolSummaryProjection
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class MarketDailyOhlcvQueryService(
	private val repository: MarketDailyOhlcvRepository,
) {
	fun listSymbols(source: String, limit: Int): List<MarketDailySymbolSummaryProjection> {
		val safeLimit = limit.coerceIn(1, 1000)
		return repository.listSymbolSummaries(source.lowercase(), PageRequest.of(0, safeLimit))
	}

	fun dailyBars(
		source: String,
		symbol: String,
		from: LocalDate?,
		to: LocalDate?,
	): List<MarketDailyOhlcv> {
		val normalizedSource = source.lowercase()
		val normalizedSymbol = symbol.trim().uppercase()
		if (normalizedSymbol.isBlank()) return emptyList()

		val defaultTo = to ?: LocalDate.now()
		val defaultFrom = from ?: defaultTo.minusYears(1)
		val safeFrom = if (defaultFrom <= defaultTo) defaultFrom else defaultTo
		val safeTo = if (defaultTo >= safeFrom) defaultTo else safeFrom

		return repository.findBySourceAndSymbolAndTradeDateBetweenOrderByTradeDateAsc(
			source = normalizedSource,
			symbol = normalizedSymbol,
			from = safeFrom,
			to = safeTo,
		)
	}
}

