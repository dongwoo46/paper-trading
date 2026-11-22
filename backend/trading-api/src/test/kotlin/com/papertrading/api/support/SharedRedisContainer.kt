package com.papertrading.api.support

import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

// 실제 Redis 컨테이너 하나를 시험 JVM 안에서 공유, 이미지는 TradingApiApplicationTests 와 같은 redis:7-alpine
object SharedRedisContainer {
    private const val REDIS_PORT = 6379

    private val container: GenericContainer<*> by lazy {
        GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(REDIS_PORT).also { it.start() }
    }

    // 호출한 시험이 끝나면 factory.destroy() 로 닫음
    fun newConnectionFactory(): LettuceConnectionFactory {
        val config = RedisStandaloneConfiguration(container.host, container.getMappedPort(REDIS_PORT))
        return LettuceConnectionFactory(config).also { it.afterPropertiesSet() }
    }

    fun newTemplate(factory: LettuceConnectionFactory) = StringRedisTemplate(factory).apply { afterPropertiesSet() }
}
