package com.papertrading.api.infrastructure.redis

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

/**
 * Redis 분산 락, SET NX PX 로 획득하고 Lua 로 «값이 내 소유자 식별값일 때만 DEL»
 * TTL 이 지난 락을 옛 소유자가 해제해 새 소유자의 락을 푸는 사고를 막음
 */
@Component
class RedisDistributedLock(private val redisTemplate: StringRedisTemplate) {

    // 얻으면 소유자 식별값, 이미 잡혀 있으면 null
    fun tryAcquire(key: String, ttlMillis: Long): String? {
        val owner = UUID.randomUUID().toString()
        val acquired = redisTemplate.opsForValue().setIfAbsent(key, owner, Duration.ofMillis(ttlMillis))
        return owner.takeIf { acquired == true }
    }

    // 소유자일 때만 해제, 이미 풀렸거나 남의 락이면 false
    fun release(key: String, owner: String): Boolean = redisTemplate.execute(RELEASE_SCRIPT, listOf(key), owner) == 1L

    companion object {
        private val RELEASE_SCRIPT = DefaultRedisScript(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end",
            Long::class.java,
        )
    }
}
