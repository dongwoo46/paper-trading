package com.papertrading.api.domain.port

interface CollectorSubscriptionPort {
    /** collector-api에 WebSocket 구독 요청. mode = "paper" | "live" */
    fun subscribe(mode: String, ticker: String)

    /** collector-api에 WebSocket 구독 해제 요청. */
    fun unsubscribe(mode: String, ticker: String)
}
