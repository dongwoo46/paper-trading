package com.papertrading.collector.common.redis

import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class RedisSetClient(
	private val redisTemplate: ReactiveStringRedisTemplate,
) {
	fun replace(key: String, values: List<String>): Mono<Void> {
		return redisTemplate.delete(key)
			.then(
				if (values.isEmpty()) Mono.empty<Void>()
				else redisTemplate.opsForSet().add(key, *values.toTypedArray()).then(),
			)
	}

	fun add(key: String, value: String): Mono<Void> {
		return redisTemplate.opsForSet().add(key, value).then()
	}

	fun remove(key: String, value: String): Mono<Void> {
		return redisTemplate.opsForSet().remove(key, value).then()
	}

	fun members(key: String): Flux<String> {
		return redisTemplate.opsForSet().members(key)
	}

	fun size(key: String): Mono<Long> {
		return redisTemplate.opsForSet().size(key).defaultIfEmpty(0L)
	}
}

