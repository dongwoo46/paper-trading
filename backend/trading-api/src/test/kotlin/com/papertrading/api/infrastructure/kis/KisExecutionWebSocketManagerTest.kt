package com.papertrading.api.infrastructure.kis

import com.papertrading.api.application.order.KisExecutionNoticeService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import java.util.concurrent.CompletableFuture

@ExtendWith(OutputCaptureExtension::class)
class KisExecutionWebSocketManagerTest {

    private val properties = KisOrderProperties().apply {
        executionWsEnabled = true
        paperWsUrl = "wss://paper.example"
        liveWsUrl = "wss://live.example"
        paperAppKey = "paper-key"
        liveAppKey = "live-key"
        wsReconnectInitialBackoffMillis = 100
        wsReconnectMaxBackoffMillis = 250
        wsReconnectMaxAttempts = 2
    }
    private val tokenManager = mockk<KisTokenManager>()
    private val transport = mockk<KisExecutionWebSocketTransport>()
    private val parser = mockk<KisExecutionNoticeParser>(relaxed = true)
    private val service = mockk<KisExecutionNoticeService>(relaxed = true)

    private val reconnectScheduler = RecordingReconnectScheduler()

    private val manager = KisExecutionWebSocketManager(
        properties = properties,
        tokenManager = tokenManager,
        transport = transport,
        parser = parser,
        noticeService = service,
        reconnectScheduler = reconnectScheduler,
    )

    @Test
    fun `start subscribes paper and live account execution channels`() {
        every { tokenManager.getWebSocketApprovalKey("paper") } returns "paper-approval"
        every { tokenManager.getWebSocketApprovalKey("live") } returns "live-approval"
        every { transport.connect(any()) } just runs

        manager.start()

        verify(exactly = 1) {
            transport.connect(match {
                it.mode == "paper" &&
                    it.url == "wss://paper.example" &&
                    it.channelId == "H0STCNI9" &&
                    it.approvalKey == "paper-approval"
            })
        }
        verify(exactly = 1) {
            transport.connect(match {
                it.mode == "live" &&
                    it.url == "wss://live.example" &&
                    it.channelId == "H0STCNI0" &&
                    it.approvalKey == "live-approval"
            })
        }
    }

    @Test
    fun `auth error reconnect fetches fresh approval key`() {
        every { tokenManager.getWebSocketApprovalKey("paper") } returnsMany listOf("first-approval", "second-approval")
        every { tokenManager.evictWebSocketApprovalKey("paper") } just runs
        every { transport.connect(any()) } just runs

        manager.connectMode("paper")
        manager.handleControlMessage("paper", KisWebSocketControlMessage.AuthError("EGW00123", "expired"))
        reconnectScheduler.runNext()

        verify(exactly = 1) { tokenManager.evictWebSocketApprovalKey("paper") }
        verify(exactly = 2) { tokenManager.getWebSocketApprovalKey("paper") }
        verify(exactly = 1) { transport.connect(match { it.approvalKey == "first-approval" }) }
        verify(exactly = 1) { transport.connect(match { it.approvalKey == "second-approval" }) }
        assertEquals(KisWebSocketConnectionStatus.CONNECTING, manager.status("paper"))
    }

    @Test
    fun `control error reconnects with fresh approval key and configured bounded backoff`() {
        every { tokenManager.getWebSocketApprovalKey("paper") } returnsMany listOf("first-approval", "second-approval")
        every { tokenManager.evictWebSocketApprovalKey("paper") } just runs
        every { transport.connect(any()) } just runs

        manager.connectMode("paper")
        manager.handleControlMessage("paper", KisWebSocketControlMessage.Error("EGW00001", "control"))

        assertEquals(listOf(ScheduledReconnect("paper", 1, 100)), reconnectScheduler.scheduled)
        reconnectScheduler.runNext()

        verify(exactly = 1) { tokenManager.evictWebSocketApprovalKey("paper") }
        verify(exactly = 1) { transport.connect(match { it.approvalKey == "second-approval" }) }
    }

    @Test
    fun `socket close and failure route to bounded reconnect attempts`() {
        every { tokenManager.getWebSocketApprovalKey("paper") } returnsMany listOf(
            "first-approval",
            "second-approval",
            "third-approval",
        )
        every { tokenManager.evictWebSocketApprovalKey("paper") } just runs
        every { transport.connect(any()) } just runs

        manager.connectMode("paper")
        manager.handleSocketClosed("paper")
        manager.handleTransportFailure("paper", RuntimeException("network"))
        manager.handleTransportFailure("paper", RuntimeException("network-again"))

        assertEquals(
            listOf(
                ScheduledReconnect("paper", 1, 100),
                ScheduledReconnect("paper", 2, 200),
            ),
            reconnectScheduler.scheduled,
        )
        verify(exactly = 2) { tokenManager.evictWebSocketApprovalKey("paper") }
        assertEquals(KisWebSocketConnectionStatus.DISCONNECTED, manager.status("paper"))
    }

    @Test
    fun `credential-bearing control message values are not logged`(output: CapturedOutput) {
        every { tokenManager.evictWebSocketApprovalKey("paper") } just runs

        manager.handleControlMessage(
            "paper",
            KisWebSocketControlMessage.AuthError(
                code = "EGW00123",
                message = "approval_key=secret-approval access_token=secret-token appsecret=secret-app",
            ),
        )

        assertFalse(output.out.contains("secret-approval"))
        assertFalse(output.out.contains("secret-token"))
        assertFalse(output.out.contains("secret-app"))
    }

    @Test
    fun `initial transport connect failure completion calls request failure callback`() {
        val client = mockk<StandardWebSocketClient>()
        val failure = IllegalStateException("handshake failed")
        every {
            client.execute(any<WebSocketHandler>(), "wss://paper.example")
        } returns CompletableFuture.failedFuture(failure)

        var capturedFailure: Throwable? = null
        val transport = SpringKisExecutionWebSocketTransport(client)

        transport.connect(
            KisExecutionWebSocketConnectionRequest(
                mode = "paper",
                url = "wss://paper.example",
                appKey = "paper-key",
                approvalKey = "paper-approval",
                channelId = "H0STCNI9",
                onMessage = {},
                onControlMessage = {},
                onConnected = {},
                onClosed = {},
                onFailure = { capturedFailure = it },
            )
        )

        assertSame(failure, capturedFailure)
    }

    private data class ScheduledReconnect(
        val mode: String,
        val attempt: Int,
        val delayMillis: Long,
    )

    private class RecordingReconnectScheduler : KisExecutionWebSocketReconnectScheduler {
        val scheduled = mutableListOf<ScheduledReconnect>()
        private val actions = ArrayDeque<() -> Unit>()

        override fun schedule(mode: String, attempt: Int, delayMillis: Long, action: () -> Unit) {
            scheduled += ScheduledReconnect(mode, attempt, delayMillis)
            actions += action
        }

        fun runNext() {
            actions.removeFirst().invoke()
        }
    }
}
