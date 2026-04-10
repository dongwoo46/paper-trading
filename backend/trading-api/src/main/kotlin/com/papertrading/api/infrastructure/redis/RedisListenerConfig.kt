package com.papertrading.api.infrastructure.redis

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer

@Configuration
class RedisListenerConfig {

    @Bean
    fun redisMessageListenerContainer(
        connectionFactory: RedisConnectionFactory,
        quoteEventListener: QuoteEventListener,
    ): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(connectionFactory)
        // quote:{ticker} 패턴 구독 → 모든 종목 시세 이벤트 수신
        container.addMessageListener(quoteEventListener, PatternTopic("quote:*"))
        return container
    }
}