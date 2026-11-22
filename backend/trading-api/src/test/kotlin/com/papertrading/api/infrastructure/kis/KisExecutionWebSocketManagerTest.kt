package com.papertrading.api.infrastructure.kis

import com.papertrading.api.application.order.BrokerExecutionNoticeService
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.TradingMode
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.CompletableFuture

// devkit:test-change 서식 정리(린트 DEC-06, 사람 승인 계획 2026-11-15) — 기존 단언 내용 불변
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
    private val service = mockk<BrokerExecutionNoticeService>(relaxed = true)

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
            transport.connect(
                match {
                    it.mode == "paper" &&
                        it.url == "wss://paper.example" &&
                        it.channelId == "H0STCNI9" &&
                        it.approvalKey == "paper-approval"
                },
            )
        }
        verify(exactly = 1) {
            transport.connect(
                match {
                    it.mode == "live" &&
                        it.url == "wss://live.example" &&
                        it.channelId == "H0STCNI0" &&
                        it.approvalKey == "live-approval"
                },
            )
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
    fun `rejected execution notice is logged with order details and keeps the session open`(output: CapturedOutput) {
        val notice = KisExecutionNotice(
            mode = TradingMode.KIS_PAPER,
            channelId = "H0STCNI9",
            externalOrderId = "0000123",
            externalExecutionId = "KIS_PAPER:50000000-01:0000123:0002",
            ticker = "005930",
            side = OrderSide.BUY,
            executedQty = BigDecimal("7"),
            executedPrice = BigDecimal("70000"),
            executedAt = Instant.parse("2026-11-15T01:00:00Z"),
            accountNumber = "50000000",
            accountProductCode = "01",
        )
        every { tokenManager.getWebSocketApprovalKey("paper") } returns "paper-approval"
        val request = slot<KisExecutionWebSocketConnectionRequest>()
        every { transport.connect(capture(request)) } just runs
        every { parser.parse("raw-notice", "paper") } returns notice
        every { service.handle(notice.toBrokerExecutionNotice()) } throws IllegalArgumentException(
            "체결 수량이 주문 잔여 수량을 넘습니다: orderId=10, executedQty=7, remainingQty=6",
        )
        manager.connectMode("paper")

        request.captured.onMessage("raw-notice")

        assertTrue(output.out.contains("orderId=10"), output.out)
        assertTrue(output.out.contains("remainingQty=6"), output.out)
        assertFalse(output.out.contains("50000000"), "account number must be masked")
        assertEquals(KisWebSocketConnectionStatus.CONNECTING, manager.status("paper"))
        assertTrue(reconnectScheduler.scheduled.isEmpty(), "rejected notice must not trigger a reconnect")
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
            ),
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
