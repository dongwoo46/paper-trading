package com.papertrading.collector.api.kis.dto

import com.papertrading.collector.source.kis.ws.SubscriptionChangeResult

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

fun SubscriptionChangeResult.toResponse(subscriptions: Map<String, List<String>>): KisSubscriptionChangeResponse {
	return KisSubscriptionChangeResponse(
		status = status.name.lowercase(),
		mode = mode,
		symbol = symbol,
		totalRegistrations = totalRegistrations,
		maxRegistrations = maxRegistrations,
		subscriptions = subscriptions,
	)
}
