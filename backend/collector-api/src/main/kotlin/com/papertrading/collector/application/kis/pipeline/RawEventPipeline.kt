package com.papertrading.collector.application.kis.pipeline

import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class RawEventPipeline {
	private val log = KotlinLogging.logger {}

	fun publish(source: String, payload: String, receivedAt: Instant = Instant.now()) {
		// TODO: 실제 파이프라인(Kafka, Redis Pub/Sub 등) 연결 전 임시 로깅
		log.debug { "raw-event source=$source, receivedAt=$receivedAt, payload=$payload" }
	}
}


