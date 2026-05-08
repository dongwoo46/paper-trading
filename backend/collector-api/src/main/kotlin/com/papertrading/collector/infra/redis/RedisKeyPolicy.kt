package com.papertrading.collector.infra.redis

import java.time.Duration

object RedisKeyPolicy {
    val CURRENT_TTL: Duration = Duration.ofSeconds(180)
    val BARS_TTL: Duration = Duration.ofHours(12)
    const val BARS_MAX_LEN: Long = 600L
    val FEATURE_TTL: Duration = Duration.ofMinutes(30)
    val DEBUG_TTL: Duration = Duration.ofSeconds(120)
    const val DEBUG_MAX_LEN: Long = 1200L

    fun currentKey(symbol: String): String = "agg:1m:$symbol:current"
    fun barsKey(symbol: String): String = "bars:1m:$symbol"
    fun featureKey(symbol: String, window: String): String = "feature:$symbol:$window"
    fun debugKey(symbol: String): String = "debug:tick:$symbol"
}

