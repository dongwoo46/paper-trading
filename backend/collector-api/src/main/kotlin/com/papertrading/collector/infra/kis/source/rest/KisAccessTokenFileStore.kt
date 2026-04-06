package com.papertrading.collector.infra.kis.source.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.collector.infra.kis.KisProperties
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.Clock
import java.time.Instant

@Component
class KisAccessTokenFileStore(
	private val objectMapper: ObjectMapper,
	private val properties: KisProperties,
) {
	private val log = KotlinLogging.logger {}
	private val lock = Any()
	private val clock: Clock = Clock.systemDefaultZone()

	fun findValidToken(mode: String): String? {
		val normalizedMode = mode.lowercase()
		val now = Instant.now(clock)
		val threshold = now.plusSeconds(60)
		return synchronized(lock) {
			val snapshot = readSnapshot() ?: return@synchronized null
			val token = snapshot.tokens[normalizedMode] ?: return@synchronized null
			if (token.accessToken.isBlank() || token.expiresAt.isBlank()) {
				return@synchronized null
			}
			val expiresAt = runCatching { Instant.parse(token.expiresAt) }.getOrNull() ?: return@synchronized null
			if (expiresAt.isAfter(threshold)) token.accessToken else null
		}
	}

	fun save(mode: String, accessToken: String, expiresAt: Instant) {
		val normalizedMode = mode.lowercase()
		val now = Instant.now(clock).toString()
		synchronized(lock) {
			val snapshot = readSnapshot() ?: TokenCacheSnapshot()
			val updated = snapshot.tokens.toMutableMap()
			updated[normalizedMode] = TokenCacheEntry(
				accessToken = accessToken,
				issuedAt = now,
				expiresAt = expiresAt.toString(),
			)
			writeSnapshot(TokenCacheSnapshot(tokens = updated))
		}
	}

	private fun readSnapshot(): TokenCacheSnapshot? {
		val path = cachePath()
		if (!Files.exists(path)) {
			return null
		}
		return try {
			Files.newBufferedReader(path).use { reader ->
				objectMapper.readValue(reader, TokenCacheSnapshot::class.java)
			}
		} catch (ex: Exception) {
			log.warn(ex) { "Failed to read KIS access token cache file. path=$path" }
			null
		}
	}

	private fun writeSnapshot(snapshot: TokenCacheSnapshot) {
		val path = cachePath()
		try {
			val parent = path.parent
			if (parent != null) {
				Files.createDirectories(parent)
			}
			val content = objectMapper.writeValueAsString(snapshot)
			Files.writeString(
				path,
				content,
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING,
				StandardOpenOption.WRITE,
			)
		} catch (ex: Exception) {
			log.warn(ex) { "Failed to write KIS access token cache file. path=$path" }
		}
	}

	private fun cachePath(): Path {
		return Paths.get(properties.accessTokenCacheFile)
	}
}

internal data class TokenCacheSnapshot(
	val tokens: Map<String, TokenCacheEntry> = emptyMap(),
)

internal data class TokenCacheEntry(
	val accessToken: String = "",
	val issuedAt: String = "",
	val expiresAt: String = "",
)

