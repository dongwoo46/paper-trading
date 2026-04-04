package com.papertrading.collector.pipeline

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class RawEventPipeline {
	private val log = LoggerFactory.getLogger(javaClass)

	fun publish(source: String, payload: String, receivedAt: Instant = Instant.now()) {
		log.debug("raw-event source={}, receivedAt={}, payload={}", source, receivedAt, payload)
	}
}

