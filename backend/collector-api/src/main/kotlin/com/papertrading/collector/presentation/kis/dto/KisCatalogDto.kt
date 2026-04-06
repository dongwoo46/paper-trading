package com.papertrading.collector.presentation.kis.dto

data class KisSymbolSubscriptionRequest(
	val mode: String,
	val symbol: String,
	val channel: String,
)

data class KisSymbolSubscriptionChangeResponse(
	val status: String,
	val mode: String,
	val symbol: String,
	val channel: String,
	val totalSelected: Int,
)

