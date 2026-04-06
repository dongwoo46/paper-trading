package com.papertrading.collector.infra.market.persistence

import com.papertrading.collector.domain.market.MarketDailyOhlcv
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface MarketDailyOhlcvRepository : JpaRepository<MarketDailyOhlcv, Long> {
	fun findBySourceAndSymbolOrderByTradeDateDesc(source: String, symbol: String): List<MarketDailyOhlcv>

	fun existsBySourceAndSymbolAndTradeDate(source: String, symbol: String, tradeDate: LocalDate): Boolean

	fun findBySourceAndSymbolAndTradeDateBetweenOrderByTradeDateAsc(
		source: String,
		symbol: String,
		from: LocalDate,
		to: LocalDate,
	): List<MarketDailyOhlcv>

	@Query(
		"""
		SELECT m.symbol AS symbol,
		       m.market AS market,
		       MAX(m.tradeDate) AS latestTradeDate,
		       COUNT(m.id) AS totalBars
		FROM MarketDailyOhlcv m
		WHERE m.source = :source
		GROUP BY m.symbol, m.market
		ORDER BY m.symbol
		""",
	)
	fun listSymbolSummaries(
		@Param("source") source: String,
		pageable: Pageable,
	): List<MarketDailySymbolSummaryProjection>
}

interface MarketDailySymbolSummaryProjection {
	fun getSymbol(): String
	fun getMarket(): String
	fun getLatestTradeDate(): LocalDate
	fun getTotalBars(): Long
}
