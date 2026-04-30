package com.papertrading.api.presentation.controller

import com.papertrading.api.infrastructure.sse.ExecutionSseRegistry
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID

@RestController
@RequestMapping("/api/v1/executions")
@CrossOrigin(origins = ["*"]) // TODO(prod): restrict to allowed origins before production deployment
class ExecutionSseController(
    private val registry: ExecutionSseRegistry,
) {

    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamExecutions(response: HttpServletResponse): SseEmitter {
        response.setHeader("Cache-Control", "no-cache")
        response.setHeader("X-Accel-Buffering", "no")

        val emitter = SseEmitter(300_000L)
        val clientId = UUID.randomUUID().toString()

        registry.register(clientId, emitter)

        emitter.send(SseEmitter.event().comment("connected"))

        return emitter
    }
}
