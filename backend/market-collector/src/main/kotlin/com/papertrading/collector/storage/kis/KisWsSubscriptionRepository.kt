package com.papertrading.collector.storage.kis

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface KisWsSubscriptionRepository : ReactiveCrudRepository<KisWsSubscription, Long> {
	fun findByModeOrderBySymbol(mode: String): Flux<KisWsSubscription>
	fun findByModeAndSymbol(mode: String, symbol: String): Mono<KisWsSubscription>
	fun deleteByModeAndSymbol(mode: String, symbol: String): Mono<Long>
}
