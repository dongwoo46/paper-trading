package com.papertrading.collector.infra.upbit.source.rest

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class UpbitMarketClient(
	private val webClientBuilder: WebClient.Builder,
) {
	private val webClient: WebClient = webClientBuilder.build()

	fun getAllMarkets(): Mono<JsonNode> {
		return webClient.get()
			.uri("https://api.upbit.com/v1/market/all?isDetails=false")
			.accept(MediaType.APPLICATION_JSON)
			.retrieve()
			.bodyToMono(JsonNode::class.java)
	}
}

