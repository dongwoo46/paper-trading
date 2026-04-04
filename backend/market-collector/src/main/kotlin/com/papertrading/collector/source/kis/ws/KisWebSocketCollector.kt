package com.papertrading.collector.source.kis.ws

import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.collector.pipeline.RawEventPipeline
import com.papertrading.collector.source.kis.config.KisProperties
import com.papertrading.collector.source.kis.rest.KisApprovalClient
import com.papertrading.collector.storage.kis.KisSymbolStore
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import org.springframework.web.reactive.socket.client.WebSocketClient
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.util.retry.Retry
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

@Component
class KisWebSocketCollector(
	private val properties: KisProperties,
	private val approvalClient: KisApprovalClient,
	private val objectMapper: ObjectMapper,
	private val rawEventPipeline: RawEventPipeline,
	private val symbolStore: KisSymbolStore,
) {
	private val log = LoggerFactory.getLogger(javaClass)
	private val running = AtomicBoolean(false)
	private val webSocketClient: WebSocketClient = ReactorNettyWebSocketClient()
	private val connections: MutableMap<String, Disposable> = ConcurrentHashMap()
	private val outboundSinks: MutableMap<String, Sinks.Many<String>> = ConcurrentHashMap()
	private val approvalKeys: MutableMap<String, String> = ConcurrentHashMap()
	private val trIds: List<String> by lazy { properties.resolvedTrIds() }

	fun start() {
		if (!properties.enabled) {
			log.info("KIS collector is disabled by configuration")
			return
		}
		if (!running.compareAndSet(false, true)) {
			return
		}

		val modes = properties.normalizedModes()
		if (modes.isEmpty()) {
			log.warn("KIS collector has no valid mode. Use modes: [paper] or [live] or [paper, live]")
			running.set(false)
			return
		}

		for (mode in modes) {
			if (properties.appKeyFor(mode).isBlank() || properties.appSecretFor(mode).isBlank()) {
				log.warn("KIS collector mode={} is enabled but key/secret is missing", mode)
				continue
			}
			symbolStore.initRestSymbols(mode).subscribe()
			startMode(mode)
		}
	}

	fun stop() {
		running.set(false)
		connections.values.forEach { it.dispose() }
		connections.clear()
		outboundSinks.clear()
		approvalKeys.clear()
	}

	@PreDestroy
	fun onDestroy() {
		stop()
	}

	private fun startMode(mode: String) {
		connections[mode] = symbolStore.initWsSymbols(mode)
			.then(Mono.defer { connectOnce(mode) })
			.retryWhen(
				Retry.backoff(Long.MAX_VALUE, properties.reconnectMinDelay)
					.maxBackoff(properties.reconnectMaxDelay)
					.doBeforeRetry { signal ->
						log.warn(
							"KIS reconnect mode={}, attempt={}, reason={}",
							mode,
							signal.totalRetries() + 1,
							signal.failure().message ?: "unknown",
						)
					},
			)
			.subscribe(
				{},
				{ error -> log.error("KIS collector terminated for mode={}", mode, error) },
			)
	}

	private fun connectOnce(mode: String): Mono<Void> {
		return Mono.zip(
			approvalClient.issueApprovalKey(mode),
			symbolStore.listWsSymbols(mode),
		).flatMap { tuple ->
			val approvalKey = tuple.t1
			val symbols = tuple.t2
			val endpoint = URI(properties.websocketUrlFor(mode))
			val outboundSink = Sinks.many().multicast().onBackpressureBuffer<String>()
			outboundSinks[mode] = outboundSink
			approvalKeys[mode] = approvalKey

			webSocketClient.execute(endpoint) { session ->
				log.info("Connected to KIS websocket mode={}, url={}", mode, properties.websocketUrlFor(mode))
				val subscribeMessages = buildSubscribeMessages(
					approvalKey = approvalKey,
					symbols = symbols,
					trIds = trIds,
					subscribe = true,
				)
				val sender = session.send(
					Flux.concat(
						Flux.fromIterable(subscribeMessages),
						outboundSink.asFlux(),
					)
						.map { payload -> session.textMessage(payload) },
				)

				val receiver = session.receive()
					.handle<String> { message, sink ->
						val decoded = decodeMessage(message)
						if (decoded != null) {
							sink.next(decoded)
						}
					}
					.doOnNext { rawEventPipeline.publish(source = "kis-$mode", payload = it) }
					.then()

				sender.then(receiver)
			}
		}
			.doFinally { signal ->
				outboundSinks.remove(mode)
				approvalKeys.remove(mode)
				log.warn("KIS websocket disconnected, mode={}, signal={}", mode, signal)
			}
			.then(Mono.error(IllegalStateException("KIS session ended for mode=$mode")))
	}

	private fun buildSubscribeMessages(
		approvalKey: String,
		symbols: List<String>,
		trIds: List<String>,
		subscribe: Boolean,
	): List<String> {
		val trType = if (subscribe) "1" else "2"
		val requests = mutableListOf<Map<String, Any>>()
		for (symbol in symbols) {
			for (trId in trIds) {
				requests += mapOf(
					"header" to mapOf(
						"approval_key" to approvalKey,
						"custtype" to properties.custType,
						"tr_type" to trType,
						"content-type" to "utf-8",
					),
					"body" to mapOf(
						"input" to mapOf(
							"tr_id" to trId,
							"tr_key" to symbol,
						),
					),
				)
			}
		}
		return requests.map { objectMapper.writeValueAsString(it) }
	}

	fun listSubscriptions(): Mono<Map<String, List<String>>> {
		val modes = properties.normalizedModes()
		return Flux.fromIterable(modes)
			.flatMap { mode -> symbolStore.listWsSymbols(mode).map { symbols -> mode to symbols } }
			.collectMap({ it.first }, { it.second })
			.map { it.toSortedMap() }
	}

	fun addSubscription(mode: String, symbol: String): Mono<SubscriptionChangeResult> {
		val normalizedMode = mode.lowercase()
		val normalizedSymbol = symbol.trim()
		val modes = properties.normalizedModes()

		if (normalizedSymbol.isBlank()) {
			return symbolStore.countWsSymbols(modes).map { count ->
				SubscriptionChangeResult(
					status = SubscriptionChangeStatus.INVALID_SYMBOL,
					mode = normalizedMode,
					symbol = normalizedSymbol,
					totalRegistrations = totalRegistrations(count),
					maxRegistrations = properties.maxRealtimeRegistrations,
				)
			}
		}
		if (!modes.contains(normalizedMode)) {
			return symbolStore.countWsSymbols(modes).map { count ->
				SubscriptionChangeResult(
					status = SubscriptionChangeStatus.INVALID_MODE,
					mode = normalizedMode,
					symbol = normalizedSymbol,
					totalRegistrations = totalRegistrations(count),
					maxRegistrations = properties.maxRealtimeRegistrations,
				)
			}
		}

		return symbolStore.countWsSymbols(modes).flatMap { count ->
			val currentRegistrations = totalRegistrations(count)
			if (currentRegistrations + trIds.size > properties.maxRealtimeRegistrations) {
				return@flatMap Mono.just(
					SubscriptionChangeResult(
						status = SubscriptionChangeStatus.LIMIT_EXCEEDED,
						mode = normalizedMode,
						symbol = normalizedSymbol,
						totalRegistrations = currentRegistrations,
						maxRegistrations = properties.maxRealtimeRegistrations,
					),
				)
			}

			symbolStore.addWsSymbol(normalizedMode, normalizedSymbol).map { added ->
				if (added) {
					emitSubscriptionChange(normalizedMode, listOf(normalizedSymbol), subscribe = true)
				}
				SubscriptionChangeResult(
					status = if (added) SubscriptionChangeStatus.ADDED else SubscriptionChangeStatus.ALREADY_EXISTS,
					mode = normalizedMode,
					symbol = normalizedSymbol,
					totalRegistrations = if (added) currentRegistrations + trIds.size else currentRegistrations,
					maxRegistrations = properties.maxRealtimeRegistrations,
				)
			}
		}
	}

	fun removeSubscription(mode: String, symbol: String): Mono<SubscriptionChangeResult> {
		val normalizedMode = mode.lowercase()
		val normalizedSymbol = symbol.trim()
		val modes = properties.normalizedModes()

		if (normalizedSymbol.isBlank()) {
			return symbolStore.countWsSymbols(modes).map { count ->
				SubscriptionChangeResult(
					status = SubscriptionChangeStatus.INVALID_SYMBOL,
					mode = normalizedMode,
					symbol = normalizedSymbol,
					totalRegistrations = totalRegistrations(count),
					maxRegistrations = properties.maxRealtimeRegistrations,
				)
			}
		}
		if (!modes.contains(normalizedMode)) {
			return symbolStore.countWsSymbols(modes).map { count ->
				SubscriptionChangeResult(
					status = SubscriptionChangeStatus.INVALID_MODE,
					mode = normalizedMode,
					symbol = normalizedSymbol,
					totalRegistrations = totalRegistrations(count),
					maxRegistrations = properties.maxRealtimeRegistrations,
				)
			}
		}

		return symbolStore.countWsSymbols(modes).flatMap { count ->
			val currentRegistrations = totalRegistrations(count)
			symbolStore.removeWsSymbol(normalizedMode, normalizedSymbol).map { removed ->
				if (removed) {
					emitSubscriptionChange(normalizedMode, listOf(normalizedSymbol), subscribe = false)
				}
				SubscriptionChangeResult(
					status = if (removed) SubscriptionChangeStatus.REMOVED else SubscriptionChangeStatus.NOT_FOUND,
					mode = normalizedMode,
					symbol = normalizedSymbol,
					totalRegistrations = if (removed) (currentRegistrations - trIds.size).coerceAtLeast(0) else currentRegistrations,
					maxRegistrations = properties.maxRealtimeRegistrations,
				)
			}
		}
	}

	private fun emitSubscriptionChange(mode: String, symbols: List<String>, subscribe: Boolean) {
		val sink = outboundSinks[mode] ?: return
		val approvalKey = approvalKeys[mode] ?: return
		val messages = buildSubscribeMessages(
			approvalKey = approvalKey,
			symbols = symbols,
			trIds = trIds,
			subscribe = subscribe,
		)
		for (message in messages) {
			sink.tryEmitNext(message)
		}
	}

	private fun decodeMessage(message: WebSocketMessage): String? {
		if (message.type != WebSocketMessage.Type.BINARY && message.type != WebSocketMessage.Type.TEXT) {
			return null
		}
		val buffer = message.payload
		val bytes = ByteArray(buffer.readableByteCount())
		buffer.read(bytes)
		return String(bytes, StandardCharsets.UTF_8)
	}

	private fun totalRegistrations(symbolCount: Int): Int {
		return symbolCount * trIds.size
	}
}

data class SubscriptionChangeResult(
	val status: SubscriptionChangeStatus,
	val mode: String,
	val symbol: String,
	val totalRegistrations: Int,
	val maxRegistrations: Int,
)

enum class SubscriptionChangeStatus {
	ADDED,
	REMOVED,
	ALREADY_EXISTS,
	NOT_FOUND,
	INVALID_MODE,
	INVALID_SYMBOL,
	LIMIT_EXCEEDED,
}
