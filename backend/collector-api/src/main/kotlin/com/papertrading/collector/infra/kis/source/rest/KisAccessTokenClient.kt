package com.papertrading.collector.infra.kis.source.rest

import com.fasterxml.jackson.annotation.JsonProperty
import com.papertrading.collector.infra.kis.KisProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class KisAccessTokenClient(
	private val properties: KisProperties,
	private val rateLimiter: KisRateLimiter,
	private val tokenRedisStore: KisTokenRedisStore,
	private val tokenDbStore: KisTokenDbStore,
	private val webClientBuilder: WebClient.Builder,
) {
	private val webClient: WebClient = webClientBuilder.build()
	private val clock: Clock = Clock.systemDefaultZone()
	private val inFlightByMode: MutableMap<String, Mono<String>> = ConcurrentHashMap()
	private val localTokenByMode: MutableMap<String, LocalToken> = ConcurrentHashMap()

	fun issueAccessToken(mode: String): Mono<String> {
		val normalizedMode = mode.lowercase()
		val local = localTokenByMode[normalizedMode]
		if (local != null && local.expiresAt.isAfter(Instant.now(clock))) {
			return Mono.just(local.token)
		}

		val redisToken = tokenRedisStore.findValidToken(normalizedMode)
		if (!redisToken.isNullOrBlank()) {
			return Mono.just(redisToken)
		}

		val dbToken = tokenDbStore.findValid(normalizedMode)
		if (dbToken != null) {
			tokenRedisStore.save(normalizedMode, dbToken.token, dbToken.expiresAt)
			return Mono.just(dbToken.token)
		}

		val inFlight = inFlightByMode.computeIfAbsent(normalizedMode) {
			val request = TokenRequest(
				grantType = "client_credentials",
				appkey = properties.appKeyFor(normalizedMode),
				appsecret = properties.appSecretFor(normalizedMode),
			)

			rateLimiter.acquireApproval(normalizedMode)
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
							localTokenByMode[normalizedMode] = LocalToken(token, expiresAt)
							tokenRedisStore.save(normalizedMode, token, expiresAt)
							tokenDbStore.save(normalizedMode, token, expiresAt)
							Mono.just(token)
						},
				)
				.doFinally { inFlightByMode.remove(normalizedMode) }
				.cache()
		}

		return inFlight
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

private data class LocalToken(
	val token: String,
	val expiresAt: Instant,
)

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



