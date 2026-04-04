package com.papertrading.collector.source.upbit.collector

import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.collector.pipeline.RawEventPipeline
import com.papertrading.collector.source.upbit.config.UpbitProperties
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import org.springframework.web.reactive.socket.client.WebSocketClient
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

@Component
class UpbitWebSocketCollector(
	private val properties: UpbitProperties,
	private val objectMapper: ObjectMapper,
	private val rawEventPipeline: RawEventPipeline,
) {
	private val log = LoggerFactory.getLogger(javaClass)
	private val running = AtomicBoolean(false)
	private val webSocketClient: WebSocketClient = ReactorNettyWebSocketClient()
	private var connection: Disposable? = null

	fun start() {
		if (!properties.enabled) {
			log.info("Upbit collector is disabled by configuration")
			return
		}
		if (!running.compareAndSet(false, true)) {
			return
		}

		connection = Mono.defer { connectOnce() }
			.retryWhen(
				Retry.backoff(Long.MAX_VALUE, properties.reconnectMinDelay)
					.maxBackoff(properties.reconnectMaxDelay)
					.doBeforeRetry { signal ->
						log.warn(
							"Upbit reconnect attempt={}, reason={}",
							signal.totalRetries() + 1,
							signal.failure().message ?: "unknown",
						)
					},
			)
			.subscribe(
				{},
				{ error ->
					running.set(false)
					log.error("Upbit collector terminated", error)
				},
			)
	}

	fun stop() {
		running.set(false)
		connection?.dispose()
	}

	@PreDestroy
	fun onDestroy() {
		stop()
	}

	private fun connectOnce(): Mono<Void> {
		val endpoint = URI(properties.url)
		val subscribePayload = buildSubscribePayload()

		return webSocketClient.execute(endpoint) { session ->
			log.info("Connected to Upbit websocket: {}", properties.url)

			val sender = session.send(
				Mono.just(session.binaryMessage { factory -> factory.wrap(subscribePayload) }),
			)

			val receiver = session.receive()
				.handle<String> { message, sink ->
					val decoded = decodeMessage(message)
					if (decoded != null) {
						sink.next(decoded)
					}
				}
				.doOnNext { payload ->
					if (shouldLogPayload(payload)) {
						log.info("Upbit payload market={}: {}", properties.logMarket, payload)
					}
					rawEventPipeline.publish(source = "upbit", payload = payload)
				}
				.then()

			sender.then(receiver)
		}
			.doFinally { signal ->
				log.warn("Upbit websocket disconnected, signal={}", signal)
			}
			.then(Mono.error(IllegalStateException("Upbit session ended")))
	}

	private fun buildSubscribePayload(): ByteArray {
		val message = mutableListOf<Map<String, Any>>()
		message += mapOf("ticket" to "market-collector-${UUID.randomUUID()}")

		val markets = properties.markets
		for (channel in properties.channels) {
			message += mapOf(
				"type" to channel,
				"codes" to markets,
				"isOnlyRealtime" to true,
			)
		}

		if (properties.simpleFormat) {
			message += mapOf("format" to "SIMPLE")
		}

		return objectMapper.writeValueAsBytes(message)
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

	private fun shouldLogPayload(payload: String): Boolean {
		if (!properties.logReceivedPayload) {
			return false
		}
		val market = properties.logMarket
		return payload.contains("\"cd\":\"$market\"") || payload.contains("\"code\":\"$market\"")
	}
}
