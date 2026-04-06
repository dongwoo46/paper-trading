package com.papertrading.collector.presentation.upbit.dto

import com.papertrading.collector.domain.upbit.UpbitMarketCatalog

data class UpbitMarketResponse(
	val market: String,
	val name: String,
	val marketGroup: String,
	val enabled: Boolean,
	val isDefault: Boolean,
)

data class UpbitMarketSelectionRequest(
	val market: String,
)

data class UpbitMarketSelectionChangeResponse(
	val status: String,
	val market: String,
	val totalSelected: Int,
)

fun UpbitMarketCatalog.toResponse(): UpbitMarketResponse {
	return UpbitMarketResponse(
		market = market,
		name = name,
		marketGroup = marketGroup,
		enabled = enabled,
		isDefault = isDefault,
	)
}

