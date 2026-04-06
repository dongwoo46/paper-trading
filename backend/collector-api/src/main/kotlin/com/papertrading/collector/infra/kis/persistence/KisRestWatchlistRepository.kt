package com.papertrading.collector.infra.kis.persistence

import com.papertrading.collector.domain.kis.KisRestWatchlist
import org.springframework.data.jpa.repository.JpaRepository

interface KisRestWatchlistRepository : JpaRepository<KisRestWatchlist, Long> {
	fun findByModeOrderBySymbol(mode: String): List<KisRestWatchlist>

	fun existsByModeAndSymbol(mode: String, symbol: String): Boolean

	fun deleteByModeAndSymbol(mode: String, symbol: String): Int
}

