package com.papertrading.collector.infra.kis.source.rest

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class KisTokenRedisStore(
    private val redisTemplate: StringRedisTemplate,
) {
    private fun key(mode: String) = "kis:token:$mode"

    fun findValidToken(mode: String): String? =
        redisTemplate.opsForValue().get(key(mode))

    fun save(mode: String, token: String, expiresAt: Instant) {
        val ttl = Duration.between(Instant.now(), expiresAt)
        if (ttl.isNegative || ttl.isZero) return
        redisTemplate.opsForValue().set(key(mode), token, ttl)
    }
}

