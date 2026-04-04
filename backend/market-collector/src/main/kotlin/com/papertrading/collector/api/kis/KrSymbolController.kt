package com.papertrading.collector.api.kis

import com.papertrading.collector.api.kis.dto.KrSymbolResponse
import com.papertrading.collector.api.kis.dto.toResponse
import com.papertrading.collector.storage.kis.KrSymbolRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/symbols/kr")
class KrSymbolController(
	private val krSymbolRepository: KrSymbolRepository,
) {
	@GetMapping
	fun search(
		@RequestParam(name = "query", required = false, defaultValue = "") query: String,
		@RequestParam(name = "market", required = false, defaultValue = "") market: String,
		@RequestParam(name = "limit", required = false, defaultValue = "50") limit: Int,
	): Mono<List<KrSymbolResponse>> {
		val normalizedQuery = query.trim()
		val normalizedMarket = market.trim().uppercase()
		val safeLimit = limit.coerceIn(1, 200)

		return krSymbolRepository.search(
			query = normalizedQuery,
			market = normalizedMarket,
			limit = safeLimit,
		)
			.map { it.toResponse() }
			.collectList()
	}
}
