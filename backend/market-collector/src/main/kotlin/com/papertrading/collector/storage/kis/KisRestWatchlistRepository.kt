package com.papertrading.collector.storage.kis

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface KisRestWatchlistRepository : ReactiveCrudRepository<KisRestWatchlist, Long> {
	fun findByModeOrderBySymbol(mode: String): Flux<KisRestWatchlist>
	fun findByModeAndSymbol(mode: String, symbol: String): Mono<KisRestWatchlist>
	fun deleteByModeAndSymbol(mode: String, symbol: String): Mono<Long>
}
