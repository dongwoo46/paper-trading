package com.papertrading.collector.storage.kis

import com.papertrading.collector.common.redis.RedisSetClient
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class KisSymbolStore(
	private val wsRepo: KisWsSubscriptionRepository,
	private val restRepo: KisRestWatchlistRepository,
	private val redisSetClient: RedisSetClient,
) {
	fun initWsSymbols(mode: String): Mono<Void> {
		return wsRepo.findByModeOrderBySymbol(mode)
			.map { it.symbol }
			.collectList()
			.flatMap { symbols -> redisSetClient.replace(wsKey(mode), symbols) }
	}

	fun initRestSymbols(mode: String): Mono<Void> {
		return restRepo.findByModeOrderBySymbol(mode)
			.map { it.symbol }
			.collectList()
			.flatMap { symbols -> redisSetClient.replace(restKey(mode), symbols) }
	}

	fun listWsSymbols(mode: String): Mono<List<String>> {
		return redisSetClient.members(wsKey(mode)).collectSortedList()
	}

	fun countWsSymbols(modes: List<String>): Mono<Int> {
		return Flux.fromIterable(modes)
			.flatMap { mode -> redisSetClient.size(wsKey(mode)) }
			.reduce(0L) { a, b -> a + b }
			.map { it.toInt() }
	}

	fun addWsSymbol(mode: String, symbol: String): Mono<Boolean> {
		return wsRepo.save(KisWsSubscription(mode = mode, symbol = symbol))
			.flatMap { redisSetClient.add(wsKey(mode), symbol).thenReturn(true) }
			.onErrorResume(DataIntegrityViolationException::class.java) { Mono.just(false) }
	}

	fun removeWsSymbol(mode: String, symbol: String): Mono<Boolean> {
		return wsRepo.deleteByModeAndSymbol(mode, symbol)
			.flatMap { deleted ->
				if (deleted > 0) redisSetClient.remove(wsKey(mode), symbol).thenReturn(true)
				else Mono.just(false)
			}
	}

	fun listRestSymbols(mode: String): Mono<List<String>> {
		return redisSetClient.members(restKey(mode)).collectSortedList()
	}

	fun addRestSymbol(mode: String, symbol: String): Mono<Boolean> {
		return restRepo.save(KisRestWatchlist(mode = mode, symbol = symbol))
			.flatMap { redisSetClient.add(restKey(mode), symbol).thenReturn(true) }
			.onErrorResume(DataIntegrityViolationException::class.java) { Mono.just(false) }
	}

	fun removeRestSymbol(mode: String, symbol: String): Mono<Boolean> {
		return restRepo.deleteByModeAndSymbol(mode, symbol)
			.flatMap { deleted ->
				if (deleted > 0) redisSetClient.remove(restKey(mode), symbol).thenReturn(true)
				else Mono.just(false)
			}
	}

	private fun wsKey(mode: String): String = "kis:ws:$mode"
	private fun restKey(mode: String): String = "kis:rest:$mode"
}