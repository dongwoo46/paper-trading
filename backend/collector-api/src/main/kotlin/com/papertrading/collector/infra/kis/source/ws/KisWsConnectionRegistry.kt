package com.papertrading.collector.infra.kis.source.ws

import com.papertrading.collector.application.kis.dto.WsHealthSnapshot
import com.papertrading.collector.domain.kis.WsConnectionStatus
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

@Component
class KisWsConnectionRegistry {

    private data class ModeState(
        val status: WsConnectionStatus,
        val lastConnectedAt: Instant?,
        val reconnectAttempts: Long,
    )

    private val states: ConcurrentHashMap<String, AtomicReference<ModeState>> = ConcurrentHashMap()

    fun markConnected(mode: String) {
        getOrCreate(mode).set(
            ModeState(
                status = WsConnectionStatus.CONNECTED,
                lastConnectedAt = Instant.now(),
                reconnectAttempts = 0,
            ),
        )
    }

    fun markReconnecting(mode: String, attempt: Long) {
        getOrCreate(mode).updateAndGet { current ->
            ModeState(
                status = WsConnectionStatus.RECONNECTING,
                lastConnectedAt = current?.lastConnectedAt,
                reconnectAttempts = attempt,
            )
        }
    }

    fun markDisconnected(mode: String) {
        getOrCreate(mode).updateAndGet { current ->
            ModeState(
                status = WsConnectionStatus.DISCONNECTED,
                lastConnectedAt = current?.lastConnectedAt,
                reconnectAttempts = current?.reconnectAttempts ?: 0,
            )
        }
    }

    fun snapshot(modes: List<String>): List<WsHealthSnapshot> {
        return modes.map { mode ->
            val state = states[mode]?.get()
            WsHealthSnapshot(
                mode = mode,
                status = state?.status ?: WsConnectionStatus.DISCONNECTED,
                lastConnectedAt = state?.lastConnectedAt,
                reconnectAttempts = state?.reconnectAttempts ?: 0,
            )
        }
    }

    private fun getOrCreate(mode: String): AtomicReference<ModeState> {
        return states.computeIfAbsent(mode) {
            AtomicReference(
                ModeState(
                    status = WsConnectionStatus.DISCONNECTED,
                    lastConnectedAt = null,
                    reconnectAttempts = 0,
                ),
            )
        }
    }
}
