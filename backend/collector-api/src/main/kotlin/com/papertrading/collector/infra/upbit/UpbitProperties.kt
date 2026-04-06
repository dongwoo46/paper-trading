package com.papertrading.collector.infra.upbit

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@ConfigurationProperties(prefix = "collector.source.upbit")
class UpbitProperties {
	var enabled: Boolean = true
	var url: String = "wss://api.upbit.com/websocket/v1"
	var markets: List<String> = listOf("KRW-BTC")
	var channels: List<String> = listOf("ticker")
	var simpleFormat: Boolean = true
	var logReceivedPayload: Boolean = true
	var logMarket: String = "KRW-BTC"
	var reconnectMinDelay: Duration = Duration.ofSeconds(1)
	var reconnectMaxDelay: Duration = Duration.ofSeconds(30)
}

