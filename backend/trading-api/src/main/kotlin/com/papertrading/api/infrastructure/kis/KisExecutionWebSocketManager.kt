package com.papertrading.api.infrastructure.kis

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.papertrading.api.application.order.KisExecutionNoticeService
import mu.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.Executors
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class KisExecutionWebSocketConnectionRequest(
    val mode: String,
    val url: String,
    val appKey: String,
    val approvalKey: String,
    val channelId: String,
    val onMessage: (String) -> Unit,
    val onControlMessage: (KisWebSocketControlMessage) -> Unit,
    val onConnected: () -> Unit,
    val onClosed: () -> Unit,
    val onFailure: (Throwable) -> Unit,
)

interface KisExecutionWebSocketTransport {
    fun connect(request: KisExecutionWebSocketConnectionRequest)
}

interface KisExecutionWebSocketReconnectScheduler {
    fun schedule(mode: String, attempt: Int, delayMillis: Long, action: () -> Unit)
}

enum class KisWebSocketConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
}

sealed class KisWebSocketControlMessage {
    data class AuthError(val code: String, val message: String) : KisWebSocketControlMessage()
    data class Error(val code: String, val message: String) : KisWebSocketControlMessage()
}

@Component
class ThreadingKisExecutionWebSocketReconnectScheduler : KisExecutionWebSocketReconnectScheduler {
    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "kis-execution-ws-reconnect").apply { isDaemon = true }
    }

    override fun schedule(mode: String, attempt: Int, delayMillis: Long, action: () -> Unit) {
        executor.schedule(action, delayMillis, TimeUnit.MILLISECONDS)
    }
}

@Component
class SpringKisExecutionWebSocketTransport(
    private val client: StandardWebSocketClient = StandardWebSocketClient(),
) : KisExecutionWebSocketTransport {
    private val objectMapper = jacksonObjectMapper()

    override fun connect(request: KisExecutionWebSocketConnectionRequest) {
        val handler = object : TextWebSocketHandler() {
            override fun afterConnectionEstablished(session: WebSocketSession) {
                request.onConnected()
                val subscribe = mapOf(
                    "header" to mapOf(
                        "approval_key" to request.approvalKey,
                        "custtype" to "P",
                        "tr_type" to "1",
                        "content-type" to "utf-8",
                    ),
                    "body" to mapOf(
                        "input" to mapOf(
                            "tr_id" to request.channelId,
                            "tr_key" to request.appKey,
                        )
                    )
                )
                session.sendMessage(TextMessage(objectMapper.writeValueAsString(subscribe)))
            }

            override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
                val controlMessage = parseControlMessage(message.payload)
                if (controlMessage != null) {
                    request.onControlMessage(controlMessage)
                    return
                }
                request.onMessage(message.payload)
            }

            override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
                request.onClosed()
            }

            override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
                request.onFailure(exception)
            }
        }

        client.execute(handler, request.url).whenComplete { _, error ->
            if (error != null) {
                request.onFailure(error)
            }
        }
    }

    private fun parseControlMessage(payload: String): KisWebSocketControlMessage? = runCatching {
        val tree = objectMapper.readTree(payload)
        val rtCode = tree.path("rt_cd").asText(null)
        if (rtCode == null || rtCode == "0") return null

        val code = tree.path("msg_cd").asText("UNKNOWN")
        val message = tree.path("msg1").asText("")
        if (code.contains("AUTH", ignoreCase = true) || code.startsWith("EGW001")) {
            KisWebSocketControlMessage.AuthError(code, message)
        } else {
            KisWebSocketControlMessage.Error(code, message)
        }
    }.getOrNull()
}

@Component
class KisExecutionWebSocketManager(
    private val properties: KisOrderProperties,
    private val tokenManager: KisTokenManager,
    private val transport: KisExecutionWebSocketTransport,
    private val parser: KisExecutionNoticeParser,
    private val noticeService: KisExecutionNoticeService,
    private val reconnectScheduler: KisExecutionWebSocketReconnectScheduler,
) {
    private val log = KotlinLogging.logger {}
    private val statuses = ConcurrentHashMap<String, KisWebSocketConnectionStatus>()
    private val reconnectAttempts = ConcurrentHashMap<String, Int>()

    @EventListener(ApplicationReadyEvent::class)
    fun start() {
        if (!properties.executionWsEnabled) return
        connectMode("paper")
        connectMode("live")
    }

    fun connectMode(mode: String) {
        reconnectAttempts[mode] = 0
        connectMode(mode, resetAttempts = false)
    }

    private fun connectMode(mode: String, resetAttempts: Boolean) {
        if (resetAttempts) reconnectAttempts[mode] = 0
        statuses[mode] = KisWebSocketConnectionStatus.CONNECTING
        val request = KisExecutionWebSocketConnectionRequest(
            mode = mode,
            url = properties.wsUrl(mode),
            appKey = properties.appKey(mode),
            approvalKey = tokenManager.getWebSocketApprovalKey(mode),
            channelId = properties.executionChannel(mode),
            onMessage = { raw -> handleRawMessage(mode, raw) },
            onControlMessage = { control -> handleControlMessage(mode, control) },
            onConnected = { handleConnected(mode) },
            onClosed = { handleSocketClosed(mode) },
            onFailure = { error -> handleTransportFailure(mode, error) },
        )
        transport.connect(request)
    }

    fun handleControlMessage(mode: String, message: KisWebSocketControlMessage) {
        when (message) {
            is KisWebSocketControlMessage.AuthError -> {
                log.warn { "KIS WebSocket auth error: mode=$mode, code=${message.code}, message=${sanitizeControlMessage(message.message)}" }
                reconnect(mode)
            }
            is KisWebSocketControlMessage.Error -> {
                log.warn { "KIS WebSocket error: mode=$mode, code=${message.code}, message=${sanitizeControlMessage(message.message)}" }
                reconnect(mode)
            }
        }
    }

    fun handleSocketClosed(mode: String) {
        log.warn { "KIS WebSocket closed: mode=$mode" }
        reconnect(mode)
    }

    fun handleTransportFailure(mode: String, error: Throwable) {
        log.warn { "KIS WebSocket transport failure: mode=$mode, error=${error::class.simpleName}" }
        reconnect(mode)
    }

    fun status(mode: String): KisWebSocketConnectionStatus =
        statuses[mode] ?: KisWebSocketConnectionStatus.DISCONNECTED

    private fun handleConnected(mode: String) {
        reconnectAttempts[mode] = 0
        statuses[mode] = KisWebSocketConnectionStatus.CONNECTED
    }

    private fun reconnect(mode: String) {
        val nextAttempt = (reconnectAttempts[mode] ?: 0) + 1
        if (nextAttempt > properties.wsReconnectMaxAttempts) {
            statuses[mode] = KisWebSocketConnectionStatus.DISCONNECTED
            log.error { "KIS WebSocket reconnect attempts exhausted: mode=$mode, attempts=${properties.wsReconnectMaxAttempts}" }
            return
        }

        reconnectAttempts[mode] = nextAttempt
        statuses[mode] = KisWebSocketConnectionStatus.DISCONNECTED
        tokenManager.evictWebSocketApprovalKey(mode)
        val delayMillis = reconnectDelayMillis(nextAttempt)
        reconnectScheduler.schedule(mode, nextAttempt, delayMillis) {
            connectMode(mode, resetAttempts = false)
        }
    }

    private fun reconnectDelayMillis(attempt: Int): Long {
        var delay = properties.wsReconnectInitialBackoffMillis.coerceAtLeast(0)
        repeat(attempt - 1) {
            delay = (delay * 2).coerceAtMost(properties.wsReconnectMaxBackoffMillis)
        }
        return delay.coerceAtMost(properties.wsReconnectMaxBackoffMillis)
    }

    private fun handleRawMessage(mode: String, rawMessage: String) {
        val notice = parser.parse(rawMessage, mode) ?: return
        noticeService.handle(notice)
    }

    private fun sanitizeControlMessage(message: String): String =
        message.replace(Regex("(?i)(approval_key|access_token|appsecret|app_secret|secretkey|authorization)=\\S+"), "$1=<redacted>")
}
