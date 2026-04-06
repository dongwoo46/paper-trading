package com.papertrading.collector.infra.fred.persistence

import com.papertrading.collector.domain.fred.FredSeriesCatalog
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FredSeriesCatalogRepository : JpaRepository<FredSeriesCatalog, Long> {
	@Query(
		"""
		SELECT f
		FROM FredSeriesCatalog f
		WHERE (:category = '' OR lower(f.category) = lower(:category))
		  AND (:frequency = '' OR lower(f.frequency) = lower(:frequency))
		  AND (
		    :query = ''
		    OR lower(f.seriesId) LIKE lower(concat('%', :query, '%'))
		    OR lower(f.title) LIKE lower(concat('%', :query, '%'))
		  )
		  AND (
		    :status = 'all'
		    OR (:status = 'subscribed' AND f.enabled = true)
		    OR (:status = 'unsubscribed' AND f.enabled = false)
		  )
		ORDER BY f.seriesId
		""",
	)
	fun search(
		@Param("query") query: String,
		@Param("category") category: String,
		@Param("frequency") frequency: String,
		@Param("status") status: String,
		pageable: Pageable,
	): List<FredSeriesCatalog>

	fun findByEnabledTrueOrderBySeriesId(): List<FredSeriesCatalog>

	fun countByEnabledTrue(): Long

	fun existsBySeriesId(seriesId: String): Boolean

	fun findBySeriesId(seriesId: String): FredSeriesCatalog?

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE FredSeriesCatalog f SET f.enabled = true WHERE f.seriesId = :seriesId AND f.enabled = false")
	fun enableBySeriesId(@Param("seriesId") seriesId: String): Int

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE FredSeriesCatalog f SET f.enabled = false WHERE f.seriesId = :seriesId AND f.enabled = true")
	fun disableBySeriesId(@Param("seriesId") seriesId: String): Int
}
