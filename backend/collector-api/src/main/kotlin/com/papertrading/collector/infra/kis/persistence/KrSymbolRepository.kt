package com.papertrading.collector.infra.kis.persistence

import com.papertrading.collector.domain.kis.KrSymbol
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface KrSymbolRepository : JpaRepository<KrSymbol, Long> {
	@Query(
		"""
		SELECT k
		FROM KrSymbol k
		WHERE (:market = '' OR k.market = :market)
		  AND (
		    :query = ''
		    OR lower(k.symbol) LIKE lower(concat('%', :query, '%'))
		    OR lower(k.name) LIKE lower(concat('%', :query, '%'))
		  )
		ORDER BY k.symbol
		""",
	)
	fun search(
		@Param("query") query: String,
		@Param("market") market: String,
		pageable: Pageable,
	): List<KrSymbol>
}

