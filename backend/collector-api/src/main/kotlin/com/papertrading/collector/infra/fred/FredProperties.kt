package com.papertrading.collector.infra.fred

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "collector.source.fred")
class FredProperties {
	var enabled: Boolean = false
	var baseUrl: String = "https://api.stlouisfed.org/fred"
	var apiKey: String = ""
	var pollingEnabled: Boolean = true
	var pollingIntervalMillis: Long = 3_600_000
	var pollingInitialDelayMillis: Long = 60_000
	var pollingLimitPerSeries: Int = 60
	var pollingRequestIntervalMillis: Long = 1_500
	var pollingMaxRetries: Int = 3
}
