package com.papertrading.collector.infra.kis

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@ConfigurationProperties(prefix = "collector.source.kis")
class KisProperties {
	var enabled: Boolean = true
	var modes: List<String> = listOf("paper")
	var paperTokenUrl: String = "https://openapivts.koreainvestment.com:29443/oauth2/tokenP"
	var paperApprovalUrl: String = "https://openapivts.koreainvestment.com:29443/oauth2/Approval"
	var paperWebsocketUrl: String = "ws://ops.koreainvestment.com:31000"
	var paperRestBaseUrl: String = "https://openapivts.koreainvestment.com:29443"
	var liveTokenUrl: String = "https://openapi.koreainvestment.com:9443/oauth2/tokenP"
	var liveApprovalUrl: String = "https://openapi.koreainvestment.com:9443/oauth2/Approval"
	var liveWebsocketUrl: String = "ws://ops.koreainvestment.com:21000"
	var liveRestBaseUrl: String = "https://openapi.koreainvestment.com:9443"
	var accessTokenCacheFile: String = "./data/kis-access-tokens.json"
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
	var restPollingEnabled: Boolean = true
	var restPollingIntervalMillis: Long = 60000
	var restPollingInitialDelayMillis: Long = 0
	var maxRealtimeRegistrations: Int = 41

	data class ModeConfig(
		val appKey: String,
		val appSecret: String,
		val tokenUrl: String,
		val approvalUrl: String,
		val websocketUrl: String,
		val restBaseUrl: String,
	)

	fun normalizedModes(): List<String> {
		return modes.map { it.lowercase() }.filter { it == "paper" || it == "live" }.distinct()
	}

	fun configFor(mode: String): ModeConfig = if (mode == "live") {
		ModeConfig(liveAppKey, liveAppSecret, liveTokenUrl, liveApprovalUrl, liveWebsocketUrl, liveRestBaseUrl)
	} else {
		ModeConfig(paperAppKey, paperAppSecret, paperTokenUrl, paperApprovalUrl, paperWebsocketUrl, paperRestBaseUrl)
	}

	fun appKeyFor(mode: String) = configFor(mode).appKey
	fun appSecretFor(mode: String) = configFor(mode).appSecret
	fun approvalUrlFor(mode: String) = configFor(mode).approvalUrl
	fun tokenUrlFor(mode: String) = configFor(mode).tokenUrl
	fun restBaseUrlFor(mode: String) = configFor(mode).restBaseUrl
	fun websocketUrlFor(mode: String) = configFor(mode).websocketUrl

	fun resolvedTrIds(): List<String> {
		val normalized = trIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
		if (normalized.isNotEmpty()) {
			return normalized
		}
		return listOf(trId)
	}
}

