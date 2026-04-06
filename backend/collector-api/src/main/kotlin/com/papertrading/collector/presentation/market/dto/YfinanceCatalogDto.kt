package com.papertrading.collector.presentation.market.dto

import com.papertrading.collector.domain.market.YfinanceSymbolCatalog
import java.time.LocalDate
import java.time.LocalDateTime

data class YfinanceSymbolResponse(
	val ticker: String,
	val name: String,
	val market: String,
	val enabled: Boolean,
	val isDefault: Boolean,
	val fetchedUntilDate: LocalDate?,
	val lastCollectedAt: LocalDateTime?,
)

data class YfinanceSelectionRequest(
	val ticker: String,
)

data class YfinanceSelectionChangeResponse(
	val status: String,
	val ticker: String,
	val totalSelected: Int,
)

data class YfinanceCollectionStatusRequest(
	val ticker: String,
	val fetchedUntilDate: LocalDate,
)

data class YfinanceCollectionStatusResponse(
	val status: String,
	val ticker: String,
	val fetchedUntilDate: LocalDate,
)

fun YfinanceSymbolCatalog.toResponse(): YfinanceSymbolResponse {
	return YfinanceSymbolResponse(
		ticker = ticker,
		name = name,
		market = market,
		enabled = enabled,
		isDefault = isDefault,
		fetchedUntilDate = fetchedUntilDate,
		lastCollectedAt = lastCollectedAt,
	)
}
