package com.papertrading.collector.api.kis

import com.papertrading.collector.api.kis.dto.KisSubscriptionChangeResponse
import com.papertrading.collector.api.kis.dto.KisSubscriptionRequest
import com.papertrading.collector.api.kis.dto.toResponse
import com.papertrading.collector.source.kis.ws.KisWebSocketCollector
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/kis/ws/subscriptions")
class KisSubscriptionController(
	private val kisWebSocketCollector: KisWebSocketCollector,
) {

	@GetMapping
	fun list(): Mono<Map<String, List<String>>> {
		return kisWebSocketCollector.listSubscriptions()
	}

	@PostMapping
	fun add(@RequestBody request: KisSubscriptionRequest): Mono<ResponseEntity<KisSubscriptionChangeResponse>> {
		return kisWebSocketCollector.addSubscription(request.mode, request.symbol)
			.flatMap { result ->
				kisWebSocketCollector.listSubscriptions()
					.map { subscriptions -> ResponseEntity.ok(result.toResponse(subscriptions)) }
			}
	}

	@DeleteMapping
	fun remove(@RequestBody request: KisSubscriptionRequest): Mono<ResponseEntity<KisSubscriptionChangeResponse>> {
		return kisWebSocketCollector.removeSubscription(request.mode, request.symbol)
			.flatMap { result ->
				kisWebSocketCollector.listSubscriptions()
					.map { subscriptions -> ResponseEntity.ok(result.toResponse(subscriptions)) }
			}
	}
}
