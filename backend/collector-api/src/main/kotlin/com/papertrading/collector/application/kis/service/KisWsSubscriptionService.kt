package com.papertrading.collector.application.kis.service

import com.papertrading.collector.domain.kis.KisWsSubscription
import com.papertrading.collector.domain.kis.SubscriptionChangeStatus
import com.papertrading.collector.infra.kis.KisProperties
import com.papertrading.collector.infra.kis.persistence.KisWsSubscriptionRepository
import com.papertrading.collector.infra.redis.RedisSetClient
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KisWsSubscriptionService(
	private val repository: KisWsSubscriptionRepository,
	private val cache: RedisSetClient,
	private val properties: KisProperties,
) {
	fun initCache(mode: String) {
		val symbols = repository.findByModeOrderBySymbol(mode).map { it.symbol }
		cache.replace(cacheKey(mode), symbols)
	}

	fun listSymbols(mode: String): List<String> {
		return cache.members(cacheKey(mode))
	}

	fun listSymbolsPerMode(): Map<String, List<String>> {
		return properties.normalizedModes()
			.associateWith { mode -> listSymbols(mode) }
			.toSortedMap()
	}

	fun countSymbols(modes: List<String>): Int {
		return modes.sumOf { mode -> cache.size(cacheKey(mode)).toInt() }
	}

	fun totalRegistrations(): Int {
		return countSymbols(properties.normalizedModes()) * properties.resolvedTrIds().size
	}

	@Transactional
	fun addSymbol(mode: String, symbol: String): SubscriptionChangeStatus {
		val modes = properties.normalizedModes()
		val trIds = properties.resolvedTrIds()

		if (symbol.isBlank()) return SubscriptionChangeStatus.INVALID_SYMBOL
		if (!modes.contains(mode)) return SubscriptionChangeStatus.INVALID_MODE

		val currentRegistrations = countSymbols(modes) * trIds.size
		if (currentRegistrations + trIds.size > properties.maxRealtimeRegistrations) {
			return SubscriptionChangeStatus.LIMIT_EXCEEDED
		}

		val added = try {
			if (repository.existsByModeAndSymbol(mode, symbol)) false
			else { repository.save(KisWsSubscription.create(mode = mode, symbol = symbol)); true }
		} catch (_: DataIntegrityViolationException) {
			false
		}

		if (!added) return SubscriptionChangeStatus.ALREADY_EXISTS
		cache.add(cacheKey(mode), symbol)
		return SubscriptionChangeStatus.ADDED
	}

	@Transactional
	fun removeSymbol(mode: String, symbol: String): SubscriptionChangeStatus {
		val modes = properties.normalizedModes()

		if (symbol.isBlank()) return SubscriptionChangeStatus.INVALID_SYMBOL
		if (!modes.contains(mode)) return SubscriptionChangeStatus.INVALID_MODE

		val deleted = repository.deleteByModeAndSymbol(mode, symbol)
		if (deleted <= 0) return SubscriptionChangeStatus.NOT_FOUND
		cache.remove(cacheKey(mode), symbol)
		return SubscriptionChangeStatus.REMOVED
	}

	private fun cacheKey(mode: String) = "kis:ws:$mode"
}