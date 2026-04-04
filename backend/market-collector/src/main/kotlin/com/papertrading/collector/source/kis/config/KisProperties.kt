package com.papertrading.collector.source.kis.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@ConfigurationProperties(prefix = "collector.source.kis")
class KisProperties {
	var enabled: Boolean = true
	var modes: List<String> = listOf("paper")
	var paperApprovalUrl: String = "https://openapivts.koreainvestment.com:29443/oauth2/Approval"
	var paperWebsocketUrl: String = "ws://ops.koreainvestment.com:31000"
	var liveApprovalUrl: String = "https://openapi.koreainvestment.com:9443/oauth2/Approval"
	var liveWebsocketUrl: String = "ws://ops.koreainvestment.com:21000"
	var liveAppKey: String = ""
	var liveAppSecret: String = ""
	var paperAppKey: String = ""
	var paperAppSecret: String = ""
	var custType: String = "P"
	var symbols: List<String> = emptyList()
	var trId: String = "H0STCNT0"
	var trIds: List<String> = listOf("H0STCNT0", "H0STASP0")
	var reconnectMinDelay: Duration = Duration.ofSeconds(1)
	var reconnectMaxDelay: Duration = Duration.ofSeconds(30)
	var liveRestRequestsPerSecond: Int = 20
	var paperRestRequestsPerSecond: Int = 2
	var approvalRequestsPerSecond: Int = 1
	var maxRealtimeRegistrations: Int = 41

	fun normalizedModes(): List<String> {
		return modes.map { it.lowercase() }.filter { it == "paper" || it == "live" }.distinct()
	}

	fun appKeyFor(mode: String): String {
		return if (mode == "live") liveAppKey else paperAppKey
	}

	fun appSecretFor(mode: String): String {
		return if (mode == "live") liveAppSecret else paperAppSecret
	}

	fun approvalUrlFor(mode: String): String {
		return if (mode == "live") liveApprovalUrl else paperApprovalUrl
	}

	fun websocketUrlFor(mode: String): String {
		return if (mode == "live") liveWebsocketUrl else paperWebsocketUrl
	}

	fun resolvedTrIds(): List<String> {
		val normalized = trIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
		if (normalized.isNotEmpty()) {
			return normalized
		}
		return listOf(trId)
	}
}
