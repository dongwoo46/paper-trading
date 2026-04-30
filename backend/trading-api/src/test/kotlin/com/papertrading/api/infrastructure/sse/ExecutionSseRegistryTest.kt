package com.papertrading.api.infrastructure.sse

import com.papertrading.api.presentation.dto.sse.ExecutionSseEvent
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException

class ExecutionSseRegistryTest {

    private lateinit var registry: ExecutionSseRegistry

    @BeforeEach
    fun setup() {
        registry = ExecutionSseRegistry()
    }

    @Test
    fun `register adds emitter and broadcast reaches it`() {
        val emitter = mockk<SseEmitter>(relaxed = true)
        registry.register("client-1", emitter)

        val sseEvent = sampleSseEvent()
        justRun { emitter.send(any<SseEmitter.SseEventBuilder>()) }
        registry.broadcast(sseEvent)

        verify(exactly = 1) { emitter.send(any<SseEmitter.SseEventBuilder>()) }
    }

    /** Spec: broadcast_after_remove_emitter_not_called */
    @Test
    fun `broadcast after remove — emitter not called`() {
        val emitter = mockk<SseEmitter>(relaxed = true)
        registry.register("client-1", emitter)
        registry.remove("client-1")

        registry.broadcast(sampleSseEvent())

        verify(exactly = 0) { emitter.send(any<SseEmitter.SseEventBuilder>()) }
    }

    /** Spec: broadcast_with_failed_emitter_emitter_removed_from_registry
     * Single failing emitter: broadcast must not throw, registry must be empty afterwards. */
    @Test
    fun `broadcast with failed emitter — emitter removed from registry, no exception thrown`() {
        val failingEmitter = mockk<SseEmitter>(relaxed = true)
        registry.register("client-fail", failingEmitter)

        every { failingEmitter.send(any<SseEmitter.SseEventBuilder>()) } throws IOException("broken pipe")
        justRun { failingEmitter.complete() }

        assertDoesNotThrow { registry.broadcast(sampleSseEvent()) }

        // Registry is empty — subsequent broadcast sends nothing
        val otherEmitter = mockk<SseEmitter>(relaxed = true)
        // No registration: broadcast should reach nobody
        registry.broadcast(sampleSseEvent())
        verify(exactly = 0) { otherEmitter.send(any<SseEmitter.SseEventBuilder>()) }
    }

    /** Spec: register_multiple_emitters_all_receive_broadcast (3 emitters) */
    @Test
    fun `register multiple emitters — all receive broadcast`() {
        val emitter1 = mockk<SseEmitter>(relaxed = true)
        val emitter2 = mockk<SseEmitter>(relaxed = true)
        val emitter3 = mockk<SseEmitter>(relaxed = true)

        registry.register("client-1", emitter1)
        registry.register("client-2", emitter2)
        registry.register("client-3", emitter3)

        justRun { emitter1.send(any<SseEmitter.SseEventBuilder>()) }
        justRun { emitter2.send(any<SseEmitter.SseEventBuilder>()) }
        justRun { emitter3.send(any<SseEmitter.SseEventBuilder>()) }

        registry.broadcast(sampleSseEvent())

        verify(exactly = 1) { emitter1.send(any<SseEmitter.SseEventBuilder>()) }
        verify(exactly = 1) { emitter2.send(any<SseEmitter.SseEventBuilder>()) }
        verify(exactly = 1) { emitter3.send(any<SseEmitter.SseEventBuilder>()) }
    }

    @Test
    fun `broadcast sends event to all registered emitters`() {
        val emitter1 = mockk<SseEmitter>(relaxed = true)
        val emitter2 = mockk<SseEmitter>(relaxed = true)

        registry.register("client-1", emitter1)
        registry.register("client-2", emitter2)

        val sseEvent = sampleSseEvent()
        justRun { emitter1.send(any<SseEmitter.SseEventBuilder>()) }
        justRun { emitter2.send(any<SseEmitter.SseEventBuilder>()) }

        registry.broadcast(sseEvent)

        verify(exactly = 1) { emitter1.send(any<SseEmitter.SseEventBuilder>()) }
        verify(exactly = 1) { emitter2.send(any<SseEmitter.SseEventBuilder>()) }
    }

    @Test
    fun `broadcast removes emitter when send throws exception`() {
        val failingEmitter = mockk<SseEmitter>(relaxed = true)
        val workingEmitter = mockk<SseEmitter>(relaxed = true)

        registry.register("client-fail", failingEmitter)
        registry.register("client-ok", workingEmitter)

        every { failingEmitter.send(any<SseEmitter.SseEventBuilder>()) } throws RuntimeException("connection lost")
        justRun { workingEmitter.send(any<SseEmitter.SseEventBuilder>()) }
        justRun { failingEmitter.complete() }

        val sseEvent = sampleSseEvent()
        registry.broadcast(sseEvent)

        // working emitter still receives next broadcast
        val sseEvent2 = sampleSseEvent()
        registry.broadcast(sseEvent2)

        // workingEmitter receives 2 broadcasts, failingEmitter only attempted once
        verify(exactly = 2) { workingEmitter.send(any<SseEmitter.SseEventBuilder>()) }
        verify(exactly = 1) { failingEmitter.send(any<SseEmitter.SseEventBuilder>()) }
    }

    @Test
    fun `remove prevents emitter from receiving broadcasts`() {
        val emitter = mockk<SseEmitter>(relaxed = true)
        registry.register("client-1", emitter)
        registry.remove("client-1")

        val sseEvent = sampleSseEvent()
        registry.broadcast(sseEvent)

        verify(exactly = 0) { emitter.send(any<SseEmitter.SseEventBuilder>()) }
    }

    private fun sampleSseEvent() = ExecutionSseEvent(
        executionId = 1L,
        orderId = 2L,
        ticker = "005930",
        tickerName = null,
        side = "BUY",
        quantity = "10",
        price = "75000",
        fee = "150",
        currency = "KRW",
        executedAt = "2026-04-30T08:30:00Z",
    )
}
