package com.papertrading.collector.infra.kis.source.ws

import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.collector.application.kis.pipeline.RawEventPipeline
import com.papertrading.collector.application.kis.service.KisRestWatchlistService
import com.papertrading.collector.application.kis.service.KisWsSubscriptionService
import com.papertrading.collector.infra.kis.KisProperties
import com.papertrading.collector.infra.kis.source.rest.KisApprovalClient
import jakarta.annotation.PreDestroy
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import org.springframework.web.reactive.socket.client.WebSocketClient
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
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
	private val wsSubscriptionService: KisWsSubscriptionService,
	private val restWatchlistService: KisRestWatchlistService,
) {
	private val log = KotlinLogging.logger {}
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

	fun emit(mode: String, symbols: List<String>, subscribe: Boolean) {
		val sink = outboundSinks[mode] ?: return
		val approvalKey = approvalKeys[mode] ?: return
		val messages = buildSubscribeMessages(approvalKey = approvalKey, symbols = symbols, trIds = trIds, subscribe = subscribe)
		for (message in messages) {
			sink.tryEmitNext(message)
		}
	}

	private fun startMode(mode: String) {
		connections[mode] = Mono.zip(
			Mono.fromCallable { wsSubscriptionService.initCache(mode) }.subscribeOn(Schedulers.boundedElastic()),
			Mono.fromCallable { restWatchlistService.initCache(mode) }.subscribeOn(Schedulers.boundedElastic()),
		)
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
		return approvalClient.issueApprovalKey(mode)
			.zipWith(Mono.fromCallable { wsSubscriptionService.listSymbols(mode) }.subscribeOn(Schedulers.boundedElastic()))
			.flatMap { tuple ->
				val approvalKey = tuple.t1
				val symbols = tuple.t2
				val endpoint = URI(properties.websocketUrlFor(mode))
				val outboundSink = Sinks.many().multicast().onBackpressureBuffer<String>()
				outboundSinks[mode] = outboundSink
				approvalKeys[mode] = approvalKey

				webSocketClient.execute(endpoint) { session ->
					log.info("Connected to KIS websocket mode={}, url={}", mode, properties.websocketUrlFor(mode))
					val sender = session.send(
						Flux.concat(
							Flux.fromIterable(buildSubscribeMessages(approvalKey, symbols, trIds, subscribe = true)),
							outboundSink.asFlux(),
						).map { session.textMessage(it) },
					)
					val receiver = session.receive()
						.handle<String> { message, sink ->
							val decoded = decodeMessage(message)
							if (decoded != null) sink.next(decoded)
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
		return symbols.flatMap { symbol ->
			trIds.map { trId ->
				objectMapper.writeValueAsString(
					mapOf(
						"header" to mapOf(
							"approval_key" to approvalKey,
							"custtype" to properties.custType,
							"tr_type" to trType,
							"content-type" to "utf-8",
						),
						"body" to mapOf(
							"input" to mapOf("tr_id" to trId, "tr_key" to symbol),
						),
					),
				)
			}
		}
	}

	private fun decodeMessage(message: WebSocketMessage): String? {
		if (message.type != WebSocketMessage.Type.BINARY && message.type != WebSocketMessage.Type.TEXT) return null
		val buffer = message.payload
		val bytes = ByteArray(buffer.readableByteCount())
		buffer.read(bytes)
		return String(bytes, StandardCharsets.UTF_8)
	}
}