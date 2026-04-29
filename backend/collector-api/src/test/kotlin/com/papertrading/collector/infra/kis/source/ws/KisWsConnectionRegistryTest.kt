package com.papertrading.collector.infra.kis.source.ws

import com.papertrading.collector.domain.kis.WsConnectionStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class KisWsConnectionRegistryTest {

    private lateinit var registry: KisWsConnectionRegistry

    @BeforeEach
    fun setUp() {
        registry = KisWsConnectionRegistry()
    }

    @Test
    fun `initial_state_for_new_mode_is_disconnected_with_null_lastConnectedAt_and_zero_attempts`() {
        val snapshot = registry.snapshot(listOf("paper")).first()

        assertThat(snapshot.status).isEqualTo(WsConnectionStatus.DISCONNECTED)
        assertThat(snapshot.lastConnectedAt).isNull()
        assertThat(snapshot.reconnectAttempts).isEqualTo(0)
    }

    @Test
    fun `registry_status_is_connected_after_markConnected`() {
        registry.markConnected("paper")

        val snapshot = registry.snapshot(listOf("paper")).first()
        assertThat(snapshot.status).isEqualTo(WsConnectionStatus.CONNECTED)
    }

    @Test
    fun `markConnected_sets_lastConnectedAt_non_null_and_resets_attempts_to_zero`() {
        registry.markReconnecting("paper", 5)
        registry.markConnected("paper")

        val snapshot = registry.snapshot(listOf("paper")).first()
        assertThat(snapshot.status).isEqualTo(WsConnectionStatus.CONNECTED)
        assertThat(snapshot.lastConnectedAt).isNotNull()
        assertThat(snapshot.reconnectAttempts).isEqualTo(0)
    }

    @Test
    fun `registry_resets_reconnect_attempts_after_markConnected`() {
        registry.markReconnecting("paper", 3)
        registry.markConnected("paper")

        val snapshot = registry.snapshot(listOf("paper")).first()
        assertThat(snapshot.reconnectAttempts).isEqualTo(0)
    }

    @Test
    fun `registry_stores_attempt_count_after_markReconnecting`() {
        registry.markReconnecting("paper", 5)

        val snapshot = registry.snapshot(listOf("paper")).first()
        assertThat(snapshot.status).isEqualTo(WsConnectionStatus.RECONNECTING)
        assertThat(snapshot.reconnectAttempts).isEqualTo(5)
    }

    @Test
    fun `registry_does_not_clear_lastConnectedAt_after_markDisconnected`() {
        registry.markConnected("paper")
        val snapshotAfterConnect = registry.snapshot(listOf("paper")).first()
        val lastConnectedAt = snapshotAfterConnect.lastConnectedAt

        registry.markDisconnected("paper")

        val snapshotAfterDisconnect = registry.snapshot(listOf("paper")).first()
        assertThat(snapshotAfterDisconnect.status).isEqualTo(WsConnectionStatus.DISCONNECTED)
        assertThat(snapshotAfterDisconnect.lastConnectedAt).isEqualTo(lastConnectedAt)
    }

    @Test
    fun `snapshot_returns_disconnected_with_nulls_for_unknown_mode`() {
        val snapshots = registry.snapshot(listOf("unknown-mode"))

        assertThat(snapshots).hasSize(1)
        val snapshot = snapshots.first()
        assertThat(snapshot.mode).isEqualTo("unknown-mode")
        assertThat(snapshot.status).isEqualTo(WsConnectionStatus.DISCONNECTED)
        assertThat(snapshot.lastConnectedAt).isNull()
        assertThat(snapshot.reconnectAttempts).isEqualTo(0)
    }

    @Test
    fun `snapshot_reflects_correct_state_after_connect_disconnect_reconnect_sequence`() {
        // Initial: unknown → DISCONNECTED
        assertThat(registry.snapshot(listOf("live")).first().status)
            .isEqualTo(WsConnectionStatus.DISCONNECTED)

        // Connect
        registry.markConnected("live")
        assertThat(registry.snapshot(listOf("live")).first().status)
            .isEqualTo(WsConnectionStatus.CONNECTED)

        // Disconnect
        registry.markDisconnected("live")
        assertThat(registry.snapshot(listOf("live")).first().status)
            .isEqualTo(WsConnectionStatus.DISCONNECTED)

        // Reconnecting attempt 1
        registry.markReconnecting("live", 1)
        val snap = registry.snapshot(listOf("live")).first()
        assertThat(snap.status).isEqualTo(WsConnectionStatus.RECONNECTING)
        assertThat(snap.reconnectAttempts).isEqualTo(1)

        // Re-connected
        registry.markConnected("live")
        val finalSnap = registry.snapshot(listOf("live")).first()
        assertThat(finalSnap.status).isEqualTo(WsConnectionStatus.CONNECTED)
        assertThat(finalSnap.reconnectAttempts).isEqualTo(0)
        assertThat(finalSnap.lastConnectedAt).isNotNull()
    }

    @Test
    fun `snapshot_returns_one_entry_per_mode_in_input_order_for_multiple_modes`() {
        registry.markConnected("paper")
        registry.markReconnecting("live", 1)

        // Input order: live before paper
        val snapshots = registry.snapshot(listOf("live", "paper"))

        assertThat(snapshots).hasSize(2)
        assertThat(snapshots[0].mode).isEqualTo("live")
        assertThat(snapshots[0].status).isEqualTo(WsConnectionStatus.RECONNECTING)
        assertThat(snapshots[1].mode).isEqualTo("paper")
        assertThat(snapshots[1].status).isEqualTo(WsConnectionStatus.CONNECTED)
    }

    @Test
    fun `concurrent_calls_do_not_corrupt_state`() {
        val threadCount = 10
        val iterationsPerThread = 100
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        repeat(threadCount) { threadIndex ->
            executor.submit {
                try {
                    repeat(iterationsPerThread) { i ->
                        val mode = if (threadIndex % 2 == 0) "paper" else "live"
                        when (i % 3) {
                            0 -> registry.markConnected(mode)
                            1 -> registry.markReconnecting(mode, (i / 3).toLong() + 1)
                            2 -> registry.markDisconnected(mode)
                        }
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        // After concurrent operations, snapshot should not throw and should return valid statuses
        val snapshots = registry.snapshot(listOf("paper", "live"))
        assertThat(snapshots).hasSize(2)
        snapshots.forEach { snap ->
            assertThat(snap.status).isIn(
                WsConnectionStatus.CONNECTED,
                WsConnectionStatus.DISCONNECTED,
                WsConnectionStatus.RECONNECTING,
            )
            assertThat(snap.reconnectAttempts).isGreaterThanOrEqualTo(0)
        }
    }
}
