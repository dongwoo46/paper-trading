package com.papertrading.collector.presentation.kis

import com.papertrading.collector.presentation.kis.dto.KrSymbolResponse
import com.papertrading.collector.presentation.kis.dto.toResponse
import com.papertrading.collector.infra.kis.persistence.KrSymbolRepository
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
	): List<KrSymbolResponse> {
		val normalizedQuery = query.trim()
		val normalizedMarket = market.trim().uppercase()
		val safeLimit = limit.coerceIn(1, 200)

		return krSymbolRepository.search(
			query = normalizedQuery,
			market = normalizedMarket,
			pageable = PageRequest.of(0, safeLimit),
		)
			.map { it.toResponse() }
	}
}

