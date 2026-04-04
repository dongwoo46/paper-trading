package com.papertrading.collector.storage.kis

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface KrSymbolRepository : ReactiveCrudRepository<KrSymbol, Long> {
	@Query(
		"""
		SELECT id, symbol, name, market
		FROM kr_symbol
		WHERE (:market = '' OR market = :market)
		  AND (
		    :query = ''
		    OR symbol ILIKE ('%' || :query || '%')
		    OR name ILIKE ('%' || :query || '%')
		  )
		ORDER BY symbol
		LIMIT :limit
		""",
	)
	fun search(query: String, market: String, limit: Int): Flux<KrSymbol>
}

