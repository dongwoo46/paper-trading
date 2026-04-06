package com.papertrading.collector.infra.market.persistence

import com.papertrading.collector.domain.market.YfinanceSymbolCatalog
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface YfinanceSymbolCatalogRepository : JpaRepository<YfinanceSymbolCatalog, Long> {
	@Query(
		"""
		SELECT y
		FROM YfinanceSymbolCatalog y
		WHERE (:market = '' OR lower(y.market) = lower(:market))
		  AND (
		    :query = ''
		    OR lower(y.ticker) LIKE lower(concat('%', :query, '%'))
		    OR lower(y.name) LIKE lower(concat('%', :query, '%'))
		  )
		  AND (
		    :status = 'all'
		    OR (:status = 'subscribed' AND y.enabled = true)
		    OR (:status = 'unsubscribed' AND y.enabled = false)
		  )
		ORDER BY y.ticker
		""",
	)
	fun search(
		@Param("query") query: String,
		@Param("market") market: String,
		@Param("status") status: String,
		pageable: Pageable,
	): List<YfinanceSymbolCatalog>

	fun findByEnabledTrueOrderByTicker(): List<YfinanceSymbolCatalog>

	fun countByEnabledTrue(): Long

	fun existsByTicker(ticker: String): Boolean

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE YfinanceSymbolCatalog y SET y.enabled = true WHERE y.ticker = :ticker AND y.enabled = false")
	fun enableByTicker(@Param("ticker") ticker: String): Int

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE YfinanceSymbolCatalog y SET y.enabled = false WHERE y.ticker = :ticker AND y.enabled = true")
	fun disableByTicker(@Param("ticker") ticker: String): Int

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(
		"""
		UPDATE YfinanceSymbolCatalog y
		SET y.fetchedUntilDate = :fetchedUntilDate,
		    y.lastCollectedAt = CURRENT_TIMESTAMP
		WHERE y.ticker = :ticker
		""",
	)
	fun updateCollectionStatus(
		@Param("ticker") ticker: String,
		@Param("fetchedUntilDate") fetchedUntilDate: LocalDate,
	): Int
}
