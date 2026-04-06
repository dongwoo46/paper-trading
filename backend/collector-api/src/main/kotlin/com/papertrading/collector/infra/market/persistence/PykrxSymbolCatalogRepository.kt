package com.papertrading.collector.infra.market.persistence

import com.papertrading.collector.domain.market.PykrxSymbolCatalog
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface PykrxSymbolCatalogRepository : JpaRepository<PykrxSymbolCatalog, Long> {
	@Query(
		"""
		SELECT p
		FROM PykrxSymbolCatalog p
		WHERE (:market = '' OR lower(p.market) = lower(:market))
		  AND (
		    :query = ''
		    OR lower(p.symbol) LIKE lower(concat('%', :query, '%'))
		    OR lower(p.name) LIKE lower(concat('%', :query, '%'))
		  )
		  AND (
		    :status = 'all'
		    OR (:status = 'subscribed' AND p.enabled = true)
		    OR (:status = 'unsubscribed' AND p.enabled = false)
		  )
		ORDER BY p.symbol
		""",
	)
	fun search(
		@Param("query") query: String,
		@Param("market") market: String,
		@Param("status") status: String,
		pageable: Pageable,
	): List<PykrxSymbolCatalog>

	fun findByEnabledTrueOrderBySymbol(): List<PykrxSymbolCatalog>

	fun countByEnabledTrue(): Long

	fun existsBySymbol(symbol: String): Boolean

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE PykrxSymbolCatalog p SET p.enabled = true WHERE p.symbol = :symbol AND p.enabled = false")
	fun enableBySymbol(@Param("symbol") symbol: String): Int

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE PykrxSymbolCatalog p SET p.enabled = false WHERE p.symbol = :symbol AND p.enabled = true")
	fun disableBySymbol(@Param("symbol") symbol: String): Int

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(
		"""
		UPDATE PykrxSymbolCatalog p
		SET p.fetchedUntilDate = :fetchedUntilDate,
		    p.lastCollectedAt = CURRENT_TIMESTAMP
		WHERE p.symbol = :symbol
		""",
	)
	fun updateCollectionStatus(
		@Param("symbol") symbol: String,
		@Param("fetchedUntilDate") fetchedUntilDate: LocalDate,
	): Int
}
