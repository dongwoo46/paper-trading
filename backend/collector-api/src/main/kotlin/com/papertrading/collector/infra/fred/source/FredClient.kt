package com.papertrading.collector.infra.fred.source

import com.fasterxml.jackson.databind.JsonNode
import com.papertrading.collector.infra.fred.FredProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.time.LocalDate

@Component
class FredClient(
	private val properties: FredProperties,
	private val webClientBuilder: WebClient.Builder,
) {
	private val webClient: WebClient = webClientBuilder
		.codecs { configurer ->
			configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)
		}
		.build()

	fun getSeriesObservations(
		seriesId: String,
		limit: Int,
		observationStart: LocalDate? = null,
	): Mono<JsonNode> {
		if (!properties.enabled) {
			return Mono.error(IllegalArgumentException("FRED source is disabled"))
		}
		if (properties.apiKey.isBlank()) {
			return Mono.error(IllegalArgumentException("FRED API key is missing"))
		}

		val sortOrder = if (observationStart == null) "desc" else "asc"
		val uriBuilder = UriComponentsBuilder
			.fromUriString("${properties.baseUrl}/series/observations")
			.queryParam("series_id", seriesId)
			.queryParam("api_key", properties.apiKey)
			.queryParam("file_type", "json")
			.queryParam("sort_order", sortOrder)
			.queryParam("limit", limit)
		if (observationStart != null) {
			uriBuilder.queryParam("observation_start", observationStart)
		}
		val requestUri = uriBuilder.build(true).toUriString()

		return webClient.get()
			.uri(requestUri)
			.accept(MediaType.APPLICATION_JSON)
			.retrieve()
			.bodyToMono(JsonNode::class.java)
	}

	fun searchSeries(
		query: String,
		limit: Int,
	): Mono<JsonNode> {
		if (!properties.enabled) {
			return Mono.error(IllegalArgumentException("FRED source is disabled"))
		}
		if (properties.apiKey.isBlank()) {
			return Mono.error(IllegalArgumentException("FRED API key is missing"))
		}

		val requestUri = UriComponentsBuilder
			.fromUriString("${properties.baseUrl}/series/search")
			.queryParam("search_text", query)
			.queryParam("api_key", properties.apiKey)
			.queryParam("file_type", "json")
			.queryParam("limit", limit)
			.build(true)
			.toUriString()

		return webClient.get()
			.uri(requestUri)
			.accept(MediaType.APPLICATION_JSON)
			.retrieve()
			.bodyToMono(JsonNode::class.java)
	}

	fun getCategoryChildren(categoryId: Long): Mono<JsonNode> {
		if (!properties.enabled) {
			return Mono.error(IllegalArgumentException("FRED source is disabled"))
		}
		if (properties.apiKey.isBlank()) {
			return Mono.error(IllegalArgumentException("FRED API key is missing"))
		}

		val requestUri = UriComponentsBuilder
			.fromUriString("${properties.baseUrl}/category/children")
			.queryParam("category_id", categoryId)
			.queryParam("api_key", properties.apiKey)
			.queryParam("file_type", "json")
			.build(true)
			.toUriString()

		return webClient.get()
			.uri(requestUri)
			.accept(MediaType.APPLICATION_JSON)
			.retrieve()
			.bodyToMono(JsonNode::class.java)
	}

	fun getCategorySeries(
		categoryId: Long,
		offset: Int,
		limit: Int,
	): Mono<JsonNode> {
		if (!properties.enabled) {
			return Mono.error(IllegalArgumentException("FRED source is disabled"))
		}
		if (properties.apiKey.isBlank()) {
			return Mono.error(IllegalArgumentException("FRED API key is missing"))
		}

		val requestUri = UriComponentsBuilder
			.fromUriString("${properties.baseUrl}/category/series")
			.queryParam("category_id", categoryId)
			.queryParam("api_key", properties.apiKey)
			.queryParam("file_type", "json")
			.queryParam("offset", offset)
			.queryParam("limit", limit)
			.build(true)
			.toUriString()

		return webClient.get()
			.uri(requestUri)
			.accept(MediaType.APPLICATION_JSON)
			.retrieve()
			.bodyToMono(JsonNode::class.java)
	}
}
