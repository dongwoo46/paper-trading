package com.papertrading.collector.infra.kis.source.rest

import com.papertrading.collector.infra.kis.KisProperties
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Component
class KisRateLimiter(
	private val properties: KisProperties,
) {
	private val nextAllowedAtMillisByKey: MutableMap<String, AtomicLong> = ConcurrentHashMap()

	fun acquireApproval(mode: String): Mono<Void> {
		return acquire(key = "approval:$mode", requestsPerSecond = properties.approvalRequestsPerSecond)
	}

	fun acquireRest(mode: String): Mono<Void> {
		val rps = if (mode == "live") properties.liveRestRequestsPerSecond else properties.paperRestRequestsPerSecond
		return acquire(key = "rest:$mode", requestsPerSecond = rps)
	}

	private fun acquire(key: String, requestsPerSecond: Int): Mono<Void> {
		val safeRps = requestsPerSecond.coerceAtLeast(1)
		val intervalMillis = (1000L / safeRps).coerceAtLeast(1L)
		val gate = nextAllowedAtMillisByKey.computeIfAbsent(key) { AtomicLong(0L) }
		val now = System.currentTimeMillis()

		while (true) {
			val nextAllowed = gate.get()
			val reservedAt = if (nextAllowed <= now) now else nextAllowed
			if (gate.compareAndSet(nextAllowed, reservedAt + intervalMillis)) {
				val delayMillis = (reservedAt - now).coerceAtLeast(0L)
				if (delayMillis == 0L) {
					return Mono.empty()
				}
				return Mono.delay(Duration.ofMillis(delayMillis)).then()
			}
		}
	}
}


