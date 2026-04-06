package com.papertrading.collector.presentation.fred.dto

import com.papertrading.collector.domain.fred.FredSeriesCatalog

data class FredSeriesResponse(
	val seriesId: String,
	val title: String,
	val category: String,
	val frequency: String,
	val units: String,
	val enabled: Boolean,
	val isDefault: Boolean,
)

data class FredSeriesSelectionRequest(
	val seriesId: String,
)

data class FredSeriesSelectionChangeResponse(
	val status: String,
	val seriesId: String,
	val totalSelected: Int,
)

fun FredSeriesCatalog.toResponse(): FredSeriesResponse {
	return FredSeriesResponse(
		seriesId = seriesId,
		title = title,
		category = category,
		frequency = frequency,
		units = units,
		enabled = enabled,
		isDefault = isDefault,
	)
}

