package com.papertrading.collector.api.kis

import com.papertrading.collector.api.kis.dto.KisSubscriptionChangeResponse
import com.papertrading.collector.api.kis.dto.KisSubscriptionRequest
import com.papertrading.collector.source.kis.config.KisProperties
import com.papertrading.collector.storage.kis.KisSymbolStore
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/kis/rest/watchlist")
class KisRestWatchlistController(
	private val symbolStore: KisSymbolStore,
	private val kisProperties: KisProperties,
) {

	@GetMapping
	fun list(): Mono<Map<String, List<String>>> {
		val modes = kisProperties.normalizedModes()
		return Flux.fromIterable(modes)
			.flatMap { mode -> symbolStore.listRestSymbols(mode).map { symbols -> mode to symbols } }
			.collectMap({ it.first }, { it.second })
			.map { it.toSortedMap() }
	}

	@PostMapping
	fun add(@RequestBody request: KisSubscriptionRequest): Mono<ResponseEntity<KisSubscriptionChangeResponse>> {
		val mode = request.mode.lowercase()
		val symbol = request.symbol.trim()
		if (mode !in kisProperties.normalizedModes() || symbol.isBlank()) {
			return list().map { subscriptions ->
				ResponseEntity.ok(
					KisSubscriptionChangeResponse(
						status = "invalid_input",
						mode = mode,
						symbol = symbol,
						totalRegistrations = subscriptions[mode]?.size ?: 0,
						maxRegistrations = Int.MAX_VALUE,
						subscriptions = subscriptions,
					),
				)
			}
		}
		return symbolStore.addRestSymbol(mode, symbol)
			.flatMap { added ->
				list().map { subscriptions ->
					ResponseEntity.ok(
						KisSubscriptionChangeResponse(
							status = if (added) "added" else "already_exists",
							mode = mode,
							symbol = symbol,
							totalRegistrations = subscriptions[mode]?.size ?: 0,
							maxRegistrations = Int.MAX_VALUE,
							subscriptions = subscriptions,
						),
					)
				}
			}
	}

	@DeleteMapping
	fun remove(@RequestBody request: KisSubscriptionRequest): Mono<ResponseEntity<KisSubscriptionChangeResponse>> {
		val mode = request.mode.lowercase()
		val symbol = request.symbol.trim()
		if (mode !in kisProperties.normalizedModes() || symbol.isBlank()) {
			return list().map { subscriptions ->
				ResponseEntity.ok(
					KisSubscriptionChangeResponse(
						status = "invalid_input",
						mode = mode,
						symbol = symbol,
						totalRegistrations = subscriptions[mode]?.size ?: 0,
						maxRegistrations = Int.MAX_VALUE,
						subscriptions = subscriptions,
					),
				)
			}
		}
		return symbolStore.removeRestSymbol(mode, symbol)
			.flatMap { removed ->
				list().map { subscriptions ->
					ResponseEntity.ok(
						KisSubscriptionChangeResponse(
							status = if (removed) "removed" else "not_found",
							mode = mode,
							symbol = symbol,
							totalRegistrations = subscriptions[mode]?.size ?: 0,
							maxRegistrations = Int.MAX_VALUE,
							subscriptions = subscriptions,
						),
					)
				}
			}
	}
}

