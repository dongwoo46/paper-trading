package com.papertrading.collector.infra.kis.source.rest

import com.fasterxml.jackson.annotation.JsonProperty
import com.papertrading.collector.infra.kis.KisProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Instant

@Component
class KisAccessTokenClient(
	private val properties: KisProperties,
	private val rateLimiter: KisRateLimiter,
	private val tokenRedisStore: KisTokenRedisStore,
	private val webClientBuilder: WebClient.Builder,
) {
	private val webClient: WebClient = webClientBuilder.build()
	private val clock: Clock = Clock.systemDefaultZone()

	fun issueAccessToken(mode: String): Mono<String> {
		val normalizedMode = mode.lowercase()
		val cachedToken = tokenRedisStore.findValidToken(normalizedMode)
		if (cachedToken != null) {
			return Mono.just(cachedToken)
		}

		val request = TokenRequest(
			grantType = "client_credentials",
			appkey = properties.appKeyFor(normalizedMode),
			appsecret = properties.appSecretFor(normalizedMode),
		)

		return rateLimiter.acquireApproval(normalizedMode)
			.then(
				webClient.post()
					.uri(properties.tokenUrlFor(normalizedMode))
					.contentType(MediaType.APPLICATION_JSON)
					.bodyValue(request)
					.retrieve()
					.bodyToMono(TokenResponse::class.java)
					.flatMap { response ->
						val token = response.accessToken ?: return@flatMap Mono.empty<String>()
						val expiresAt = resolveExpiresAt(response)
						tokenRedisStore.save(normalizedMode, token, expiresAt)
						Mono.just(token)
					},
			)
	}

	private fun resolveExpiresAt(response: TokenResponse): Instant {
		val expiresInSeconds = response.expiresIn?.toLongOrNull()
		if (expiresInSeconds != null && expiresInSeconds > 0) {
			return Instant.now(clock).plusSeconds(expiresInSeconds)
		}

		val absolute = response.accessTokenTokenExpired
		if (!absolute.isNullOrBlank()) {
			val parsed = runCatching { Instant.parse(absolute) }.getOrNull()
			if (parsed != null) {
				return parsed
			}
		}

		return Instant.now(clock).plusSeconds(86_400) // KIS 토큰 기본 24h
	}
}

private data class TokenRequest(
	@JsonProperty("grant_type") val grantType: String,
	val appkey: String,
	val appsecret: String,
)

private data class TokenResponse(
	@JsonProperty("access_token") val accessToken: String?,
	@JsonProperty("expires_in") val expiresIn: String?,
	@JsonProperty("access_token_token_expired") val accessTokenTokenExpired: String?,
)

