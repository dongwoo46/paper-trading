package com.papertrading.collector.infra.kis.source.rest

import com.fasterxml.jackson.databind.JsonNode
import com.papertrading.collector.infra.kis.KisProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriComponentsBuilder
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration

@Component
class KisQuoteClient(
	private val properties: KisProperties,
	private val rateLimiter: KisRateLimiter,
	private val accessTokenClient: KisAccessTokenClient,
	private val webClientBuilder: WebClient.Builder,
) {
	private val webClient: WebClient = webClientBuilder.build()
	private fun quoteTrIdFor(mode: String): String = "FHKST01010100"

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
							.header("tr_id", quoteTrIdFor(mode))
							.header("custtype", properties.custType)
							.accept(MediaType.APPLICATION_JSON)
							.retrieve()
							.onStatus(HttpStatusCode::isError) { response ->
								response.bodyToMono(String::class.java)
									.defaultIfEmpty("")
									.flatMap { body ->
										Mono.error(
											WebClientResponseException.create(
												response.statusCode().value(),
												"KIS inquire-price failed. mode=$mode, symbol=$symbol, body=$body",
												response.headers().asHttpHeaders(),
												body.toByteArray(),
												null,
											),
										)
									}
							}
							.bodyToMono(JsonNode::class.java)
							.flatMap { body ->
								val rtCd = body.path("rt_cd").asText("")
								if (rtCd == "0") {
									Mono.just(body)
								} else {
									val msgCd = body.path("msg_cd").asText("")
									val msg1 = body.path("msg1").asText("")
									Mono.error(
										WebClientResponseException.create(
											500,
											"KIS inquire-price business failed. mode=$mode, symbol=$symbol, msg_cd=$msgCd, msg1=$msg1",
											HttpHeaders.EMPTY,
											body.toString().toByteArray(),
											null,
										),
									)
								}
							}
							.retryWhen(
								Retry.fixedDelay(3, Duration.ofMillis(1200))
									.filter { ex ->
										ex is WebClientResponseException &&
											ex.message?.contains("EGW00201") == true
									},
							),
					)
			}
	}
}



