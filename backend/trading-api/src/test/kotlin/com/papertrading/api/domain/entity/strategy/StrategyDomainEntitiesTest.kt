package com.papertrading.api.domain.entity.strategy

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.ApprovalStatus
import com.papertrading.api.domain.enums.StrategySourceType
import com.papertrading.api.domain.enums.TradingMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class StrategyDomainEntitiesTest {

    @Test
    fun strategy_create_initial_state_is_draft_and_inactive() {
        val strategy = createStrategy()

        assertEquals(ApprovalStatus.DRAFT, strategy.approvalStatus)
        assertFalse(strategy.isActive)
    }

    @Test
    fun strategy_review_approve_activate_flow_works() {
        val strategy = createStrategy()

        strategy.requestReview()
        strategy.approve()
        strategy.activate()

        assertEquals(ApprovalStatus.APPROVED, strategy.approvalStatus)
        assertTrue(strategy.isActive)
    }

    @Test
    fun strategy_cannot_approve_without_pending_review() {
        val strategy = createStrategy()

        assertThrows(IllegalStateException::class.java) { strategy.approve() }
    }

    @Test
    fun strategy_reject_requires_pending_review_and_deactivates() {
        val strategy = createStrategy()
        assertThrows(IllegalStateException::class.java) { strategy.reject() }

        strategy.requestReview()
        strategy.reject()

        assertEquals(ApprovalStatus.REJECTED, strategy.approvalStatus)
        assertFalse(strategy.isActive)
    }

    @Test
    fun strategy_activate_requires_approved() {
        val strategy = createStrategy()

        assertThrows(IllegalStateException::class.java) { strategy.activate() }
    }

    @Test
    fun strategy_activate_twice_throws() {
        val strategy = createStrategy()
        strategy.requestReview()
        strategy.approve()
        strategy.activate()

        assertThrows(IllegalStateException::class.java) { strategy.activate() }
    }

    @Test
    fun strategy_deactivate_twice_throws() {
        val strategy = createStrategy()
        strategy.requestReview()
        strategy.approve()
        strategy.activate()

        strategy.deactivate()
        assertThrows(IllegalStateException::class.java) { strategy.deactivate() }
    }

    @Test
    fun strategy_cancel_sets_terminal_cancel_state() {
        val strategy = createStrategy()
        strategy.requestReview()
        strategy.approve()
        strategy.activate()

        strategy.cancel("수동 중단")

        assertTrue(strategy.isCancelled)
        assertFalse(strategy.isActive)
        assertTrue(strategy.cancelledAt != null)
        assertEquals("수동 중단", strategy.cancelReason)
    }

    @Test
    fun strategy_cancelled_cannot_be_reviewed_approved_or_activated() {
        val strategy = createStrategy()
        strategy.cancel("운영 중단")

        assertThrows(IllegalStateException::class.java) { strategy.requestReview() }
        assertThrows(IllegalStateException::class.java) { strategy.approve() }
        assertThrows(IllegalStateException::class.java) { strategy.activate() }
    }

    @Test
    fun strategy_mark_executed_tracks_execution_count_and_last_time() {
        val strategy = createStrategy()
        strategy.requestReview()
        strategy.approve()
        strategy.activate()

        val t1 = Instant.now()
        strategy.markExecuted(t1)
        strategy.markExecuted(t1.plusSeconds(1))

        assertTrue(strategy.hasExecuted())
        assertEquals(2L, strategy.executionCount)
        assertEquals(t1.plusSeconds(1), strategy.lastExecutedAt)
    }

    @Test
    fun strategy_mark_executed_requires_active_and_not_cancelled() {
        val strategy = createStrategy()
        assertThrows(IllegalStateException::class.java) { strategy.markExecuted() }

        strategy.requestReview()
        strategy.approve()
        strategy.activate()
        strategy.cancel("중단")

        assertThrows(IllegalStateException::class.java) { strategy.markExecuted() }
    }

    @Test
    fun strategy_cancel_requires_non_blank_reason() {
        val strategy = createStrategy()

        assertThrows(IllegalArgumentException::class.java) { strategy.cancel("   ") }
        assertThrows(IllegalArgumentException::class.java) { strategy.cancel("x".repeat(501)) }
    }

    @Test
    fun strategy_request_review_allows_draft_and_rejected_only() {
        val strategy = createStrategy("s1")
        strategy.requestReview() // DRAFT -> PENDING_REVIEW

        assertThrows(IllegalStateException::class.java) { strategy.requestReview() }

        val approved = createStrategy("s2")
        approved.requestReview()
        approved.approve()
        assertThrows(IllegalStateException::class.java) { approved.requestReview() }

        val rejected = createStrategy("s3")
        rejected.requestReview()
        rejected.reject()
        rejected.requestReview()
        assertEquals(ApprovalStatus.PENDING_REVIEW, rejected.approvalStatus)
    }

    @Test
    fun strategy_rename_and_create_trim_and_validate_length() {
        val strategy = Strategy.create(
            account = createAccount(),
            name = "  alpha  ",
            sourceType = StrategySourceType.HUMAN,
            description = null
        )
        assertEquals("alpha", strategy.name)

        strategy.rename("  beta  ")
        assertEquals("beta", strategy.name)

        assertThrows(IllegalArgumentException::class.java) { strategy.rename(" ") }
        assertThrows(IllegalArgumentException::class.java) {
            Strategy.create(createAccount(), "x".repeat(201), StrategySourceType.HUMAN)
        }
    }

    @Test
    fun strategy_description_blank_becomes_null() {
        val strategy = createStrategy()

        strategy.updateDescription("  ")
        assertNull(strategy.description)
    }

    @Test
    fun strategy_update_performance_validates_ranges() {
        val strategy = createStrategy()

        strategy.updatePerformance(
            sharpeRatio = BigDecimal("1.2345"),
            maxDrawdown = BigDecimal("-0.2500"),
            winRate = BigDecimal("0.6000"),
            avgReturn = BigDecimal("0.0500")
        )

        assertEquals(BigDecimal("-0.2500"), strategy.maxDrawdown)

        assertThrows(IllegalArgumentException::class.java) {
            strategy.updatePerformance(null, BigDecimal("0.1000"), null, null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            strategy.updatePerformance(null, null, BigDecimal("1.1000"), null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            strategy.updatePerformance(null, null, null, BigDecimal("-1.1000"))
        }
    }

    @Test
    fun strategy_derivation_validates_parent_child_and_type() {
        val parent = createStrategy("parent")
        val child = createStrategy("child")

        val derivation = StrategyDerivation.create(parent, child, "fork", "note")
        assertEquals("FORK", derivation.derivationType)

        assertThrows(IllegalArgumentException::class.java) {
            StrategyDerivation.create(parent, parent, "FORK")
        }
        assertThrows(IllegalArgumentException::class.java) {
            StrategyDerivation.create(parent, child, "invalid")
        }
    }

    @Test
    fun strategy_derivation_update_note_blank_to_null() {
        val derivation = StrategyDerivation.create(createStrategy("p"), createStrategy("c"), "TUNE", "memo")

        derivation.updateNote("  ")

        assertNull(derivation.note)
    }

    @Test
    fun strategy_log_validates_level_message_and_future_time() {
        val strategy = createStrategy()
        val account = createAccount()

        val log = StrategyLog.create(strategy, account, "warn", "signal drop", "{\"key\":1}", Instant.now())
        assertEquals("WARN", log.logLevel)

        assertThrows(IllegalArgumentException::class.java) {
            StrategyLog.create(strategy, account, "debug", "test")
        }
        assertThrows(IllegalArgumentException::class.java) {
            StrategyLog.create(strategy, account, "INFO", " ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            StrategyLog.create(strategy, account, "INFO", "msg", null, Instant.now().plusSeconds(60))
        }
    }

    @Test
    fun strategy_log_update_context_blank_to_null() {
        val log = StrategyLog.create(createStrategy(), createAccount(), "INFO", "hello", "{\"a\":1}")

        log.updateContext("  ")

        assertNull(log.context)
    }

    @Test
    fun performance_snapshot_validates_period_and_ranges() {
        val strategy = createStrategy()
        val snapshot = StrategyPerformanceSnapshot.create(
            strategy = strategy,
            periodStart = LocalDate.of(2026, 1, 1),
            periodEnd = LocalDate.of(2026, 1, 31),
            totalReturn = BigDecimal("0.1234"),
            sharpeRatio = BigDecimal("1.1111"),
            maxDrawdown = BigDecimal("-0.1234"),
            winRate = BigDecimal("0.5600"),
            totalTrades = 20
        )

        assertEquals(BigDecimal("0.1234"), snapshot.totalReturn)

        assertThrows(IllegalArgumentException::class.java) {
            StrategyPerformanceSnapshot.create(
                strategy,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 1, 31),
                BigDecimal.ZERO,
                null,
                null,
                null,
                0
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            StrategyPerformanceSnapshot.create(
                strategy,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                BigDecimal.ZERO,
                null,
                BigDecimal("0.1"),
                null,
                0
            )
        }
    }

    @Test
    fun performance_snapshot_revise_metrics_validates_ranges() {
        val snapshot = StrategyPerformanceSnapshot.create(
            strategy = createStrategy(),
            periodStart = LocalDate.of(2026, 1, 1),
            periodEnd = LocalDate.of(2026, 1, 31),
            totalReturn = BigDecimal("0.0500"),
            sharpeRatio = null,
            maxDrawdown = BigDecimal("-0.1000"),
            winRate = BigDecimal("0.5000"),
            totalTrades = 10
        )

        snapshot.reviseMetrics(BigDecimal("0.0600"), BigDecimal("1.2"), BigDecimal("-0.0900"), BigDecimal("0.5500"), 12)
        assertEquals(BigDecimal("0.0600"), snapshot.totalReturn)

        assertThrows(IllegalArgumentException::class.java) {
            snapshot.reviseMetrics(BigDecimal("0.0100"), null, BigDecimal("-1.1000"), null, 1)
        }
    }

    @Test
    fun strategy_version_validates_fields_and_note_updates() {
        val strategy = createStrategy()

        val version = StrategyVersion.create(
            strategy = strategy,
            versionNo = 1,
            rules = "{\"rule\":\"value\"}",
            createdBy = "tester"
        )

        assertEquals(1, version.versionNo)

        assertThrows(IllegalArgumentException::class.java) {
            StrategyVersion.create(strategy, 0, "{}")
        }
        assertThrows(IllegalArgumentException::class.java) {
            StrategyVersion.create(strategy, 1, " ")
        }

        version.updateChangeNote("  ")
        assertNull(version.changeNote)
    }

    private fun createStrategy(name: String = "strategy"): Strategy {
        return Strategy.create(
            account = createAccount(),
            name = name,
            sourceType = StrategySourceType.HUMAN,
            description = "test"
        )
    }

    private fun createAccount(): Account {
        return Account.create(
            accountName = "acc",
            accountType = AccountType.STOCK,
            tradingMode = TradingMode.LOCAL,
            initialDeposit = BigDecimal("100000.0000")
        )
    }
}
