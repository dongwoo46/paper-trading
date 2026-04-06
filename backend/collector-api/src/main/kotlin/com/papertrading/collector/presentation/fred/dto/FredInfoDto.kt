package com.papertrading.collector.presentation.fred.dto

import com.fasterxml.jackson.databind.JsonNode

data class FredSeriesInfoResponse(
	val seriesId: String,
	val title: String?,
	val category: String?,
	val frequency: String?,
	val units: String?,
	val enabled: Boolean?,
	val isDefault: Boolean?,
	val observations: JsonNode?,
)

