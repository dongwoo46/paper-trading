package com.papertrading.api.infrastructure.client

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "trading.collector-api")
class CollectorApiProperties {
    var baseUrl: String = "http://localhost:8081"
    var connectTimeoutMs: Long = 2000
    var readTimeoutMs: Long = 3000
}