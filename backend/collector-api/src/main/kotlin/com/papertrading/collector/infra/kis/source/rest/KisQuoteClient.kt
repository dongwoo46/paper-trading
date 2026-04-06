package com.papertrading.collector.infra.kis.source.rest

import com.fasterxml.jackson.databind.JsonNode
import com.papertrading.collector.infra.kis.KisProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class KisQuoteClient(
	private val properties: KisProperties,
	private val rateLimiter: KisRateLimiter,
	private val accessTokenClient: KisAccessTokenClient,
	private val webClientBuilder: WebClient.Builder,
) {
	private val webClient: WebClient = webClientBuilder.build()

	fun inquirePrice(mode: String, symbol: String): Mono<JsonNode> {
		val requestUri = UriComponentsBuilder
			.fromUriString("${properties.restBaseUrlFor(mode)}/uapi/domestic-stock/v1/quotations/inquire-price")
			.queryParam("fid_cond_mrkt_div_code", "J")
			.queryParam("fid_input_iscd", symbol)
			.build(true)
			.toUriString()

		return accessTokenClient.issueAccessToken(mode)
			.flatMap { accessToken ->
				rateLimiter.acquireRest(mode)
					.then(
						webClient.get()
							.uri(requestUri)
							.header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
							.header("appkey", properties.appKeyFor(mode))
							.header("appsecret", properties.appSecretFor(mode))
							.header("tr_id", "FHKST01010100")
							.header("custtype", properties.custType)
							.accept(MediaType.APPLICATION_JSON)
							.retrieve()
							.bodyToMono(JsonNode::class.java),
					)
			}
	}
}

