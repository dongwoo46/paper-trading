package com.papertrading.collector.presentation.kis

import com.papertrading.collector.application.kis.service.KisWsSubscriptionService
import com.papertrading.collector.domain.kis.SubscriptionChangeStatus
import com.papertrading.collector.infra.kis.KisProperties
import com.papertrading.collector.infra.kis.source.ws.KisWebSocketCollector
import com.papertrading.collector.presentation.kis.dto.KisSubscriptionChangeResponse
import com.papertrading.collector.presentation.kis.dto.KisSubscriptionRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/kis/ws/subscriptions")
class KisSubscriptionController(
	private val wsSubscriptionService: KisWsSubscriptionService,
	private val kisWebSocketCollector: KisWebSocketCollector,
	private val kisProperties: KisProperties,
) {

	@GetMapping
	fun list(): Map<String, List<String>> {
		return wsSubscriptionService.listSymbolsPerMode()
	}

	@PostMapping
	fun add(@RequestBody request: KisSubscriptionRequest): ResponseEntity<KisSubscriptionChangeResponse> {
		val mode = request.mode.lowercase()
		val symbol = request.symbol.trim()
		val status = wsSubscriptionService.addSymbol(mode, symbol)
		if (status == SubscriptionChangeStatus.ADDED) {
			kisWebSocketCollector.emit(mode, listOf(symbol), subscribe = true)
		}
		return ResponseEntity.ok(buildResponse(status, mode, symbol))
	}

	@DeleteMapping
	fun remove(@RequestBody request: KisSubscriptionRequest): ResponseEntity<KisSubscriptionChangeResponse> {
		val mode = request.mode.lowercase()
		val symbol = request.symbol.trim()
		val status = wsSubscriptionService.removeSymbol(mode, symbol)
		if (status == SubscriptionChangeStatus.REMOVED) {
			kisWebSocketCollector.emit(mode, listOf(symbol), subscribe = false)
		}
		return ResponseEntity.ok(buildResponse(status, mode, symbol))
	}

	private fun buildResponse(status: SubscriptionChangeStatus, mode: String, symbol: String) =
		KisSubscriptionChangeResponse(
			status = status.name.lowercase(),
			mode = mode,
			symbol = symbol,
			totalRegistrations = wsSubscriptionService.totalRegistrations(),
			maxRegistrations = kisProperties.maxRealtimeRegistrations,
			subscriptions = wsSubscriptionService.listSymbolsPerMode(),
		)
}