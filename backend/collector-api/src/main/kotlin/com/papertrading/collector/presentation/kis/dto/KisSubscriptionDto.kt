package com.papertrading.collector.presentation.kis.dto

data class KisSubscriptionRequest(
	val mode: String,
	val symbol: String,
)

data class KisSubscriptionChangeResponse(
	val status: String,
	val mode: String,
	val symbol: String,
	val totalRegistrations: Int,
	val maxRegistrations: Int,
	val subscriptions: Map<String, List<String>>,
)