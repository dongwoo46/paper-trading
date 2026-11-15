package com.papertrading.api.infrastructure.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class RestTemplateConfig {

    @Bean
    fun restTemplate(builder: RestTemplateBuilder) = builder
        .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
        .readTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS))
        .build()

    companion object {
        // 토스 토큰 발급 최악 소요(락 TTL 과 요청 전체 상한의 근거)가 이 값에서 유도되므로 한 곳에 둠
        const val CONNECT_TIMEOUT_SECONDS = 5L
        const val READ_TIMEOUT_SECONDS = 10L
    }
}
