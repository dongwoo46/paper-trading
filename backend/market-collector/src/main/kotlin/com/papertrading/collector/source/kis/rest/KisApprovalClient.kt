package com.papertrading.collector.source.kis.rest

import com.papertrading.collector.source.kis.config.KisProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class KisApprovalClient(
	private val properties: KisProperties,
	private val rateLimiter: KisRateLimiter,
	private val webClientBuilder: WebClient.Builder,
) {
	private val webClient: WebClient = webClientBuilder.build()

	fun issueApprovalKey(mode: String): Mono<String> {
		val request = ApprovalRequest(
			grant_type = "client_credentials",
			appkey = properties.appKeyFor(mode),
			secretkey = properties.appSecretFor(mode),
		)

		return rateLimiter.acquireApproval(mode)
			.then(
				webClient.post()
					.uri(properties.approvalUrlFor(mode))
					.contentType(MediaType.APPLICATION_JSON)
					.bodyValue(request)
					.retrieve()
					.bodyToMono(ApprovalResponse::class.java)
					.mapNotNull { it.approval_key },
			)
	}
}

private data class ApprovalRequest(
	val grant_type: String,
	val appkey: String,
	val secretkey: String,
)

private data class ApprovalResponse(
	val approval_key: String?,
)

