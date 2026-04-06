package com.papertrading.collector.infra.kis.persistence

import com.papertrading.collector.domain.kis.KisWsSubscription
import org.springframework.data.jpa.repository.JpaRepository

interface KisWsSubscriptionRepository : JpaRepository<KisWsSubscription, Long> {
	fun findByModeOrderBySymbol(mode: String): List<KisWsSubscription>

	fun existsByModeAndSymbol(mode: String, symbol: String): Boolean

	fun deleteByModeAndSymbol(mode: String, symbol: String): Int
}

