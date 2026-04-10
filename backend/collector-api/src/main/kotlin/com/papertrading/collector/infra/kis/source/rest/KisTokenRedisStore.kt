package com.papertrading.collector.infra.kis.source.rest

import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

/**
 * KIS 액세스 토큰 Redis 공유 저장소
 * key: kis:token:{mode} (paper | live)
 * TTL = 토큰 만료 시간 그대로. 키 소멸 시 재발급.
 * collector-api / trading-api가 동일 키 공유 → 하루 1회 발급.
 */
@Component
class KisTokenRedisStore(
    private val redisTemplate: StringRedisTemplate,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private fun key(mode: String) = "kis:token:$mode"
    }

    fun findValidToken(mode: String): String? =
        redisTemplate.opsForValue().get(key(mode))

    fun save(mode: String, token: String, expiresAt: Instant) {
        val ttl = Duration.between(Instant.now(), expiresAt)
        if (ttl.isNegative || ttl.isZero) {
            log.warn { "KIS 토큰 이미 만료됨, Redis 저장 스킵: mode=$mode" }
            return
        }
        redisTemplate.opsForValue().set(key(mode), token, ttl)
        log.info { "KIS 토큰 Redis 저장: mode=$mode, ttl=${ttl.toSeconds()}s (${ttl.toHours()}h)" }
    }
}
