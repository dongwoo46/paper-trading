package com.papertrading.collector.infra.upbit.persistence

import com.papertrading.collector.domain.upbit.UpbitMarketCatalog
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UpbitMarketCatalogRepository : JpaRepository<UpbitMarketCatalog, Long> {
	@Query(
		"""
		SELECT u
		FROM UpbitMarketCatalog u
		WHERE (:marketGroup = '' OR lower(u.marketGroup) = lower(:marketGroup))
		  AND (
		    :query = ''
		    OR lower(u.market) LIKE lower(concat('%', :query, '%'))
		    OR lower(u.name) LIKE lower(concat('%', :query, '%'))
		  )
		  AND (
		    :status = 'all'
		    OR (:status = 'subscribed' AND u.enabled = true)
		    OR (:status = 'unsubscribed' AND u.enabled = false)
		  )
		ORDER BY u.market
		""",
	)
	fun search(
		@Param("query") query: String,
		@Param("marketGroup") marketGroup: String,
		@Param("status") status: String,
		pageable: Pageable,
	): List<UpbitMarketCatalog>

	fun findByEnabledTrueOrderByMarket(): List<UpbitMarketCatalog>

	fun countByEnabledTrue(): Long

	fun existsByMarket(market: String): Boolean

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE UpbitMarketCatalog u SET u.enabled = true WHERE u.market = :market AND u.enabled = false")
	fun enableByMarket(@Param("market") market: String): Int

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE UpbitMarketCatalog u SET u.enabled = false WHERE u.market = :market AND u.enabled = true")
	fun disableByMarket(@Param("market") market: String): Int
}
