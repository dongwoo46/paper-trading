package com.papertrading.collector.presentation.market.dto

import com.papertrading.collector.domain.market.PykrxSymbolCatalog
import java.time.LocalDate
import java.time.LocalDateTime

data class PykrxSymbolResponse(
	val symbol: String,
	val name: String,
	val market: String,
	val enabled: Boolean,
	val isDefault: Boolean,
	val fetchedUntilDate: LocalDate?,
	val lastCollectedAt: LocalDateTime?,
)

data class PykrxSelectionRequest(
	val symbol: String,
)

data class PykrxSelectionChangeResponse(
	val status: String,
	val symbol: String,
	val totalSelected: Int,
)

data class PykrxCollectionStatusRequest(
	val symbol: String,
	val fetchedUntilDate: LocalDate,
)

data class PykrxCollectionStatusResponse(
	val status: String,
	val symbol: String,
	val fetchedUntilDate: LocalDate,
)

fun PykrxSymbolCatalog.toResponse(): PykrxSymbolResponse {
	return PykrxSymbolResponse(
		symbol = symbol,
		name = name,
		market = market,
		enabled = enabled,
		isDefault = isDefault,
		fetchedUntilDate = fetchedUntilDate,
		lastCollectedAt = lastCollectedAt,
	)
}
