package com.papertrading.api.infrastructure.sse

import com.papertrading.api.presentation.dto.sse.ExecutionSseEvent
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

@Component
class ExecutionSseRegistry {

    private val log = KotlinLogging.logger {}
    private val emitters = ConcurrentHashMap<String, SseEmitter>()

    fun register(clientId: String, emitter: SseEmitter) {
        emitters[clientId] = emitter
        emitter.onCompletion { remove(clientId) }
        emitter.onTimeout { remove(clientId) }
        emitter.onError { remove(clientId) }
        log.info { "SSE client registered: clientId=$clientId, total=${emitters.size}" }
    }

    fun remove(clientId: String) {
        emitters.remove(clientId)
        log.info { "SSE client removed: clientId=$clientId, remaining=${emitters.size}" }
    }

    fun broadcast(event: ExecutionSseEvent) {
        val snapshot = emitters.toMap()
        snapshot.forEach { (clientId, emitter) ->
            try {
                emitter.send(
                    SseEmitter.event()
                        .name("execution")
                        .data(event, MediaType.APPLICATION_JSON)
                )
            } catch (e: Exception) {
                log.warn { "SSE send failed for clientId=$clientId, removing: ${e.message}" }
                remove(clientId)
                emitter.complete()
            }
        }
    }
}
