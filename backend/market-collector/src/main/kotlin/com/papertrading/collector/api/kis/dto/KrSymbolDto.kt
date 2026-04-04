package com.papertrading.collector.api.kis.dto

import com.papertrading.collector.storage.kis.KrSymbol

data class KrSymbolResponse(
	val symbol: String,
	val name: String,
	val market: String,
)

fun KrSymbol.toResponse(): KrSymbolResponse {
	return KrSymbolResponse(
		symbol = symbol,
		name = name,
		market = market,
	)
}

