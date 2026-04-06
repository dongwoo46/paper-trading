package com.papertrading.collector.infra.redis

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisSetClient(
	private val redisTemplate: StringRedisTemplate,
) {
	private val ops = redisTemplate.opsForSet()

	fun replace(key: String, values: List<String>) {
		redisTemplate.delete(key)
		if (values.isNotEmpty()) {
			ops.add(key, *values.toTypedArray())
		}
	}

	fun add(key: String, value: String) {
		ops.add(key, value)
	}

	fun remove(key: String, value: String) {
		ops.remove(key, value)
	}

	fun members(key: String): List<String> {
		return ops.members(key)?.sorted() ?: emptyList()
	}

	fun size(key: String): Long {
		return ops.size(key) ?: 0L
	}
}

