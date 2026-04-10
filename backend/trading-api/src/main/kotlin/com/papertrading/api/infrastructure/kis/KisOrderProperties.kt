package com.papertrading.api.infrastructure.kis

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "trading.kis")
class KisOrderProperties {
    var paperRestBaseUrl: String = "https://openapivts.koreainvestment.com:29443"
    var liveRestBaseUrl: String = "https://openapi.koreainvestment.com:9443"
    var paperTokenUrl: String = "https://openapivts.koreainvestment.com:29443/oauth2/tokenP"
    var liveTokenUrl: String = "https://openapi.koreainvestment.com:9443/oauth2/tokenP"
    var paperAppKey: String = ""
    var paperAppSecret: String = ""
    var liveAppKey: String = ""
    var liveAppSecret: String = ""

    fun restBaseUrl(mode: String) = if (mode == "paper") paperRestBaseUrl else liveRestBaseUrl
    fun tokenUrl(mode: String) = if (mode == "paper") paperTokenUrl else liveTokenUrl
    fun appKey(mode: String) = if (mode == "paper") paperAppKey else liveAppKey
    fun appSecret(mode: String) = if (mode == "paper") paperAppSecret else liveAppSecret
}