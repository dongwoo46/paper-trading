package com.papertrading.collector.application.kis.service

import com.papertrading.collector.domain.kis.KisRestWatchlist
import com.papertrading.collector.domain.kis.SubscriptionChangeStatus
import com.papertrading.collector.infra.kis.KisProperties
import com.papertrading.collector.infra.kis.persistence.KisRestWatchlistRepository
import com.papertrading.collector.infra.redis.RedisSetClient
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KisRestWatchlistService(
	private val repository: KisRestWatchlistRepository,
	private val cache: RedisSetClient,
	private val properties: KisProperties,
) {
	fun initCache(mode: String) {
		val symbols = repository.findByModeOrderBySymbol(mode).map { it.symbol }
		cache.replace(cacheKey(mode), symbols)
	}

	fun listSymbols(mode: String): List<String> {
		val cached = try {
			cache.members(cacheKey(mode))
		} catch (_: Exception) {
			emptyList()
		}
		if (cached.isNotEmpty()) return cached

		val dbSymbols = repository.findByModeOrderBySymbol(mode).map { it.symbol }
		if (dbSymbols.isNotEmpty()) {
			cache.replace(cacheKey(mode), dbSymbols)
		}
		return dbSymbols
	}

	@Transactional
	fun addSymbol(mode: String, symbol: String): SubscriptionChangeStatus {
		if (symbol.isBlank()) return SubscriptionChangeStatus.INVALID_SYMBOL
		if (!properties.normalizedModes().contains(mode)) return SubscriptionChangeStatus.INVALID_MODE

		val added = try {
			if (repository.existsByModeAndSymbol(mode, symbol)) false
			else { repository.save(KisRestWatchlist.create(mode = mode, symbol = symbol)); true }
		} catch (_: DataIntegrityViolationException) {
			false
		}

		if (!added) return SubscriptionChangeStatus.ALREADY_EXISTS
		cache.add(cacheKey(mode), symbol)
		return SubscriptionChangeStatus.ADDED
	}

	@Transactional
	fun removeSymbol(mode: String, symbol: String): SubscriptionChangeStatus {
		if (symbol.isBlank()) return SubscriptionChangeStatus.INVALID_SYMBOL
		if (!properties.normalizedModes().contains(mode)) return SubscriptionChangeStatus.INVALID_MODE

		val deleted = repository.deleteByModeAndSymbol(mode, symbol)
		if (deleted <= 0) return SubscriptionChangeStatus.NOT_FOUND
		cache.remove(cacheKey(mode), symbol)
		return SubscriptionChangeStatus.REMOVED
	}

	private fun cacheKey(mode: String) = "kis:rest:$mode"
}
