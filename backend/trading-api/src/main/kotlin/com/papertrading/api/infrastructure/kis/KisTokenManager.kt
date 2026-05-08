package com.papertrading.api.infrastructure.kis

import com.fasterxml.jackson.annotation.JsonProperty
import com.papertrading.api.common.exception.ExternalServiceResponseException
import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.time.Instant

/**
 * KIS 액세스 토큰 관리
 * Redis key: kis:token:{mode} — collector-api와 공유.
 * 키 없으면 KIS API 직접 발급 후 Redis 저장. TTL = 토큰 만료 시간.
 * KIS 토큰은 하루 1회만 발급 가능하므로 Redis 공유가 필수.
 */
@Component
class KisTokenManager(
    private val properties: KisOrderProperties,
    private val redisTemplate: StringRedisTemplate,
    private val restTemplate: RestTemplate,
) {
    private val log = KotlinLogging.logger {}
    private val lock = Any()

    companion object {
        private fun redisKey(mode: String) = "kis:token:$mode"
        private fun approvalRedisKey(mode: String) = "kis:approval:$mode"
    }

    fun getToken(mode: String): String {
        redisTemplate.opsForValue().get(redisKey(mode))?.let { return it }

        return synchronized(lock) {
            // double-check
            redisTemplate.opsForValue().get(redisKey(mode))
                ?: issueAndSave(mode)
        }
    }

    fun getWebSocketApprovalKey(mode: String): String {
        val key = approvalRedisKey(mode)
        redisTemplate.opsForValue().get(key)?.let { return it }

        val request = ApprovalRequest(
            grantType = "client_credentials",
            appKey = properties.appKey(mode),
            secretKey = properties.appSecret(mode),
        )
        val response = restTemplate.postForObject(
            "${properties.restBaseUrl(mode)}/oauth2/Approval",
            request,
            ApprovalResponse::class.java,
        ) ?: throw ExternalServiceResponseException("KIS WebSocket approval response is null: mode=$mode")

        val approvalKey = requireNotNull(response.approvalKey) { "approval_key null: mode=$mode" }
        redisTemplate.opsForValue().set(key, approvalKey, Duration.ofHours(20))
        log.info { "KIS WebSocket approval key stored: mode=$mode" }
        return approvalKey
    }

    fun evictWebSocketApprovalKey(mode: String) {
        redisTemplate.delete(approvalRedisKey(mode))
    }

    private fun issueAndSave(mode: String): String {
        log.info { "KIS 토큰 발급 요청: mode=$mode" }
        val request = TokenRequest(
            grantType = "client_credentials",
            appKey = properties.appKey(mode),
            appSecret = properties.appSecret(mode),
        )
        val response = restTemplate.postForObject(
            properties.tokenUrl(mode),
            request,
            TokenResponse::class.java,
        ) ?: throw ExternalServiceResponseException("KIS 토큰 발급 응답이 null: mode=$mode")

        val token = requireNotNull(response.accessToken) { "access_token null: mode=$mode" }
        val expiresAt = resolveExpiresAt(response)
        val ttl = Duration.between(Instant.now(), expiresAt)

        if (!ttl.isNegative && !ttl.isZero) {
            redisTemplate.opsForValue().set(redisKey(mode), token, ttl)
            log.info { "KIS 토큰 Redis 저장: mode=$mode, ttl=${ttl.toHours()}h" }
        }
        return token
    }

    private fun resolveExpiresAt(response: TokenResponse): Instant {
        val seconds = response.expiresIn?.toLongOrNull()
        if (seconds != null && seconds > 0) return Instant.now().plusSeconds(seconds)
        return Instant.now().plusSeconds(86400) // 기본 24h
    }

    private data class TokenRequest(
        @JsonProperty("grant_type") val grantType: String,
        @JsonProperty("appkey") val appKey: String,
        @JsonProperty("appsecret") val appSecret: String,
    )

    private data class TokenResponse(
        @JsonProperty("access_token") val accessToken: String?,
        @JsonProperty("expires_in") val expiresIn: String?,
    )

    private data class ApprovalRequest(
        @JsonProperty("grant_type") val grantType: String,
        @JsonProperty("appkey") val appKey: String,
        @JsonProperty("secretkey") val secretKey: String,
    )

    private data class ApprovalResponse(
        @JsonProperty("approval_key") val approvalKey: String?,
    )
}
