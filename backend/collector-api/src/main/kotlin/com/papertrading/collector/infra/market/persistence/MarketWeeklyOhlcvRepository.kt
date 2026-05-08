package com.papertrading.collector.infra.market.persistence

import com.papertrading.collector.domain.entity.market.MarketWeeklyOhlcv
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface MarketWeeklyOhlcvRepository : JpaRepository<MarketWeeklyOhlcv, Long> {
	fun findBySourceAndSymbolAndTradeDateBetweenOrderByTradeDateAsc(
		source: String,
		symbol: String,
		from: LocalDate,
		to: LocalDate,
	): List<MarketWeeklyOhlcv>

	fun findBySourceAndSymbolOrderByTradeDateDesc(
		source: String,
		symbol: String,
		pageable: Pageable,
	): List<MarketWeeklyOhlcv>
}
