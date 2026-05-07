package com.papertrading.api.domain.entity.account

import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.enums.TransactionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class AccountTest {

    @Test
    fun 계좌를_생성하면_초기_예수금이_가용예수금과_동일하다() {
        // given when
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))

        // then
        assertEquals(BigDecimal("100000.0000"), account.deposit)
        assertEquals(BigDecimal("100000.0000"), account.availableDeposit)
        assertZero(account.lockedDeposit)
    }

    @Test
    fun 입금하면_예수금과_가용예수금이_동시에_증가한다() {
        // given
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))

        // when
        account.addDeposit(BigDecimal("50000.0000"))

        // then
        assertEquals(BigDecimal("150000.0000"), account.deposit)
        assertEquals(BigDecimal("150000.0000"), account.availableDeposit)
    }

    @Test
    fun 출금하면_예수금과_가용예수금이_동시에_감소한다() {
        // given
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))

        // when
        account.withdraw(BigDecimal("25000.0000"))

        // then
        assertEquals(BigDecimal("75000.0000"), account.deposit)
        assertEquals(BigDecimal("75000.0000"), account.availableDeposit)
    }

    @Test
    fun 출금시_가용예수금이_부족하면_예외를_던지고_상태를_변경하지_않는다() {
        // given
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))
        account.lockDeposit(BigDecimal("90000.0000"))

        // when then
        assertThrows(IllegalArgumentException::class.java) {
            account.withdraw(BigDecimal("20000.0000"))
        }

        assertEquals(BigDecimal("100000.0000"), account.deposit)
        assertEquals(BigDecimal("10000.0000"), account.availableDeposit)
        assertEquals(BigDecimal("90000.0000"), account.lockedDeposit)
    }

    @Test
    fun 입금_금액이_0_이하면_예외를_던진다() {
        // given
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))

        // when then
        assertThrows(IllegalArgumentException::class.java) {
            account.addDeposit(BigDecimal.ZERO)
        }
        assertThrows(IllegalArgumentException::class.java) {
            account.addDeposit(BigDecimal("-1"))
        }
    }

    @Test
    fun 출금_금액이_0_이하면_예외를_던진다() {
        // given
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))

        // when then
        assertThrows(IllegalArgumentException::class.java) {
            account.withdraw(BigDecimal.ZERO)
        }
        assertThrows(IllegalArgumentException::class.java) {
            account.withdraw(BigDecimal("-1"))
        }
    }

    @Test
    fun 계좌타입과_거래모드_조합이_잘못되면_생성에_실패한다() {
        // when then
        assertThrows(IllegalArgumentException::class.java) {
            Account.create(
                accountName = "crypto-account",
                accountType = AccountType.CRYPTO,
                tradingMode = TradingMode.KIS_LIVE,
                initialDeposit = BigDecimal("1000.0000")
            )
        }
    }

    @Test
    fun recordDeposit_입금과_원장을_동시에_반환한다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))

        val ledger = account.recordDeposit(BigDecimal("50000.0000"), "idem-001", "테스트 입금")

        assertEquals(BigDecimal("150000.0000"), account.deposit)
        assertEquals(BigDecimal("150000.0000"), account.availableDeposit)
        assertEquals(TransactionType.DEPOSIT, ledger.transactionType)
        assertEquals(BigDecimal("50000.0000"), ledger.amount)
        assertEquals(BigDecimal("150000.0000"), ledger.balanceAfter)
        assertEquals("idem-001", ledger.idempotencyKey)
    }

    @Test
    fun recordWithdrawal_출금과_원장을_동시에_반환한다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))

        val ledger = account.recordWithdrawal(BigDecimal("30000.0000"), "idem-002", "테스트 출금")

        assertEquals(BigDecimal("70000.0000"), account.deposit)
        assertEquals(BigDecimal("70000.0000"), account.availableDeposit)
        assertEquals(TransactionType.WITHDRAWAL, ledger.transactionType)
        assertEquals(BigDecimal("30000.0000"), ledger.amount)
        assertEquals(BigDecimal("70000.0000"), ledger.balanceAfter)
    }

    @Test
    fun recordInitialDeposit_초기_예수금_원장을_반환한다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))

        val ledger = account.recordInitialDeposit("init-account-1")

        assertEquals(TransactionType.DEPOSIT, ledger.transactionType)
        assertEquals(BigDecimal("100000.0000"), ledger.amount)
        assertEquals("init-account-1", ledger.idempotencyKey)
    }

    @Test
    fun activate_비활성_계좌를_활성화한다() {
        val account = createAccount(initialDeposit = BigDecimal.ZERO)
        account.deactivate()

        account.activate()

        assertTrue(account.isActive)
    }

    @Test
    fun activate_이미_활성화된_계좌면_예외를_던진다() {
        val account = createAccount(initialDeposit = BigDecimal.ZERO)

        assertThrows(IllegalStateException::class.java) {
            account.activate()
        }
    }

    @Test
    fun createRiskPolicy_리스크_정책을_생성한다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))

        val policy = account.createRiskPolicy(
            maxPositionRatio = BigDecimal("0.2"),
            maxDailyLoss = BigDecimal("10000.0000"),
            maxOrderAmount = BigDecimal("50000.0000")
        )

        assertEquals(account, policy.account)
        assertEquals(BigDecimal("0.2"), policy.maxPositionRatio)
        assertTrue(policy.isActive)
    }

    @Test
    fun updateRiskPolicy_기존_정책을_비활성화하고_새_정책을_반환한다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))
        val existing = account.createRiskPolicy(
            maxPositionRatio = BigDecimal("0.2"),
            maxDailyLoss = null,
            maxOrderAmount = null
        )

        val updated = account.updateRiskPolicy(
            existing = existing,
            maxPositionRatio = BigDecimal("0.3"),
            maxDailyLoss = BigDecimal("20000.0000"),
            maxOrderAmount = null
        )

        assertFalse(existing.isActive)
        assertTrue(updated.isActive)
        assertEquals(BigDecimal("0.3"), updated.maxPositionRatio)
        assertEquals(BigDecimal("20000.0000"), updated.maxDailyLoss)
    }

    @Test
    fun createRiskPolicy_maxPositionRatio가_0_1_범위를_벗어나면_예외를_던진다() {
        val account = createAccount(initialDeposit = BigDecimal.ZERO)

        assertThrows(IllegalArgumentException::class.java) {
            account.createRiskPolicy(
                maxPositionRatio = BigDecimal("1.1"),
                maxDailyLoss = null,
                maxOrderAmount = null
            )
        }
    }

    @Test
    fun createRiskPolicy_maxPositionRatio_경계값_0과_1은_유효하다() {
        val account = createAccount(initialDeposit = BigDecimal.ZERO)

        val p0 = account.createRiskPolicy(BigDecimal.ZERO, null, null)
        val p1 = account.createRiskPolicy(BigDecimal.ONE, null, null)

        assertEquals(BigDecimal.ZERO, p0.maxPositionRatio)
        assertEquals(BigDecimal.ONE, p1.maxPositionRatio)
    }

    @Test
    fun createRiskPolicy_maxDailyLoss가_0이면_예외를_던진다() {
        val account = createAccount(initialDeposit = BigDecimal.ZERO)

        assertThrows(IllegalArgumentException::class.java) {
            account.createRiskPolicy(null, BigDecimal.ZERO, null)
        }
    }

    @Test
    fun createRiskPolicy_모두_null이면_정상_생성된다() {
        val account = createAccount(initialDeposit = BigDecimal.ZERO)

        val policy = account.createRiskPolicy(null, null, null)

        assertTrue(policy.isActive)
    }

    @Test
    fun withdraw_가용예수금_전액_출금하면_잔액이_0이된다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))

        account.withdraw(BigDecimal("100000.0000"))

        assertZero(account.deposit)
        assertZero(account.availableDeposit)
    }

    @Test
    fun recordWithdrawal_가용예수금_부족하면_예외를_던지고_상태를_변경하지_않는다() {
        val account = createAccount(initialDeposit = BigDecimal("10000.0000"))

        assertThrows(IllegalArgumentException::class.java) {
            account.recordWithdrawal(BigDecimal("20000.0000"), "idem-003")
        }

        assertEquals(BigDecimal("10000.0000"), account.deposit)
    }

    @Test
    fun deactivate_이미_비활성화된_계좌면_예외를_던진다() {
        val account = createAccount(initialDeposit = BigDecimal.ZERO)
        account.deactivate()

        assertThrows(IllegalStateException::class.java) {
            account.deactivate()
        }
    }

    @Test
    fun create_계좌명이_빈_문자열이면_예외를_던진다() {
        assertThrows(IllegalArgumentException::class.java) {
            Account.create(
                accountName = "  ",
                accountType = AccountType.STOCK,
                tradingMode = TradingMode.LOCAL,
                initialDeposit = BigDecimal.ZERO
            )
        }
    }

    @Test
    fun create_초기예수금이_음수면_예외를_던진다() {
        assertThrows(IllegalArgumentException::class.java) {
            Account.create(
                accountName = "test",
                accountType = AccountType.STOCK,
                tradingMode = TradingMode.LOCAL,
                initialDeposit = BigDecimal("-1")
            )
        }
    }

    // --- lockDeposit ---

    @Test
    fun lockDeposit_잠금하면_가용예수금이_감소하고_잠금예수금이_증가한다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))

        account.lockDeposit(BigDecimal("30000.0000"))

        assertEquals(BigDecimal("100000.0000"), account.deposit)
        assertEquals(BigDecimal("70000.0000"), account.availableDeposit)
        assertEquals(BigDecimal("30000.0000"), account.lockedDeposit)
    }

    @Test
    fun lockDeposit_전액_잠금하면_가용예수금이_0이된다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))

        account.lockDeposit(BigDecimal("100000.0000"))

        assertZero(account.availableDeposit)
        assertEquals(BigDecimal("100000.0000"), account.lockedDeposit)
    }

    @Test
    fun lockDeposit_금액이_0이면_예외를_던진다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))

        assertThrows(IllegalArgumentException::class.java) {
            account.lockDeposit(BigDecimal.ZERO)
        }
    }

    @Test
    fun lockDeposit_가용예수금_초과하면_예외를_던지고_상태를_변경하지_않는다() {
        val account = createAccount(initialDeposit = BigDecimal("10000.0000"))

        assertThrows(IllegalArgumentException::class.java) {
            account.lockDeposit(BigDecimal("20000.0000"))
        }

        assertEquals(BigDecimal("10000.0000"), account.availableDeposit)
        assertZero(account.lockedDeposit)
    }

    // --- unlockDeposit ---

    @Test
    fun unlockDeposit_해제하면_잠금예수금이_감소하고_가용예수금이_증가한다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))
        account.lockDeposit(BigDecimal("30000.0000"))

        account.unlockDeposit(BigDecimal("30000.0000"))

        assertEquals(BigDecimal("100000.0000"), account.availableDeposit)
        assertZero(account.lockedDeposit)
    }

    @Test
    fun unlockDeposit_금액이_0이면_예외를_던진다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))
        account.lockDeposit(BigDecimal("30000.0000"))

        assertThrows(IllegalArgumentException::class.java) {
            account.unlockDeposit(BigDecimal.ZERO)
        }
    }

    @Test
    fun unlockDeposit_잠금액_초과하면_예외를_던지고_상태를_변경하지_않는다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))
        account.lockDeposit(BigDecimal("30000.0000"))

        assertThrows(IllegalArgumentException::class.java) {
            account.unlockDeposit(BigDecimal("40000.0000"))
        }

        assertEquals(BigDecimal("30000.0000"), account.lockedDeposit)
    }

    // --- confirmBuy ---

    @Test
    fun confirmBuy_매수확정하면_잠금예수금과_총예수금이_감소한다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))
        account.lockDeposit(BigDecimal("30000.0000"))

        account.confirmBuy(BigDecimal("30000.0000"))

        assertEquals(BigDecimal("70000.0000"), account.deposit)
        assertEquals(BigDecimal("70000.0000"), account.availableDeposit)
        assertZero(account.lockedDeposit)
    }

    @Test
    fun confirmBuy_금액이_0이면_예외를_던진다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))
        account.lockDeposit(BigDecimal("30000.0000"))

        assertThrows(IllegalArgumentException::class.java) {
            account.confirmBuy(BigDecimal.ZERO)
        }
    }

    @Test
    fun confirmBuy_잠금액_초과하면_예외를_던지고_상태를_변경하지_않는다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))
        account.lockDeposit(BigDecimal("30000.0000"))

        assertThrows(IllegalArgumentException::class.java) {
            account.confirmBuy(BigDecimal("40000.0000"))
        }

        assertEquals(BigDecimal("100000.0000"), account.deposit)
        assertEquals(BigDecimal("30000.0000"), account.lockedDeposit)
    }

    // --- receiveSellProceeds ---

    @Test
    fun receiveSellProceeds_매도대금이_입금되면_예수금과_가용예수금이_증가한다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))

        account.receiveSellProceeds(BigDecimal("50000.0000"))

        assertEquals(BigDecimal("150000.0000"), account.deposit)
        assertEquals(BigDecimal("150000.0000"), account.availableDeposit)
    }

    @Test
    fun receiveSellProceeds_금액이_0이면_예외를_던진다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))

        assertThrows(IllegalArgumentException::class.java) {
            account.receiveSellProceeds(BigDecimal.ZERO)
        }
    }

    // --- rename ---

    @Test
    fun rename_계좌명을_변경한다() {
        val account = createAccount(initialDeposit = BigDecimal.ZERO)

        account.rename("새 계좌명")

        assertEquals("새 계좌명", account.accountName)
    }

    @Test
    fun rename_빈_문자열이면_예외를_던진다() {
        val account = createAccount(initialDeposit = BigDecimal.ZERO)

        assertThrows(IllegalArgumentException::class.java) {
            account.rename("  ")
        }
    }

    // --- record* 원장 메서드 ---

    @Test
    fun recordBuyLock_잠금과_원장을_동시에_반환한다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))

        val ledger = account.recordBuyLock(BigDecimal("30000.0000"), 1L, "buy-lock-1")

        assertEquals(BigDecimal("70000.0000"), account.availableDeposit)
        assertEquals(BigDecimal("30000.0000"), account.lockedDeposit)
        assertEquals(TransactionType.BUY_LOCK, ledger.transactionType)
        assertEquals(BigDecimal("30000.0000"), ledger.amount)
        assertEquals(BigDecimal("70000.0000"), ledger.balanceAfter)
    }

    @Test
    fun recordBuyUnlock_해제와_원장을_동시에_반환한다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))
        account.lockDeposit(BigDecimal("30000.0000"))

        val ledger = account.recordBuyUnlock(BigDecimal("30000.0000"), 1L, "buy-unlock-1")

        assertEquals(BigDecimal("100000.0000"), account.availableDeposit)
        assertZero(account.lockedDeposit)
        assertEquals(TransactionType.BUY_UNLOCK, ledger.transactionType)
    }

    @Test
    fun recordBuyExecution_매수확정과_원장을_동시에_반환한다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))
        account.lockDeposit(BigDecimal("30000.0000"))

        val ledger = account.recordBuyExecution(BigDecimal("30000.0000"), 1L, 10L, "buy-exec-10")

        assertEquals(BigDecimal("70000.0000"), account.deposit)
        assertZero(account.lockedDeposit)
        assertEquals(TransactionType.BUY_EXECUTE, ledger.transactionType)
        assertEquals(BigDecimal("30000.0000"), ledger.amount)
        assertEquals(10L, ledger.refExecutionId)
    }

    @Test
    fun recordSellExecution_원장을_반환하고_잔액은_호출자가_관리한다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))
        account.receiveSellProceeds(BigDecimal("50000.0000"))

        val ledger = account.recordSellExecution(BigDecimal("50000.0000"), 1L, 10L, "sell-exec-10")

        assertEquals(TransactionType.SELL_EXECUTE, ledger.transactionType)
        assertEquals(BigDecimal("50000.0000"), ledger.amount)
        assertEquals(BigDecimal("150000.0000"), ledger.balanceAfter)
    }

    @Test
    fun recordFee_수수료_원장을_반환한다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))

        val ledger = account.recordFee(BigDecimal("500.0000"), 1L, 10L, "fee-10")

        assertEquals(TransactionType.FEE, ledger.transactionType)
        assertEquals(BigDecimal("500.0000"), ledger.amount)
        assertEquals(BigDecimal("100000.0000"), ledger.balanceAfter)
    }

    @Test
    fun recordSettlement_정산입금과_원장을_동시에_반환한다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))

        val ledger = account.recordDepositWithLedger(BigDecimal("50000.0000"), "settlement-1")

        assertEquals(BigDecimal("150000.0000"), account.deposit)
        assertEquals(BigDecimal("150000.0000"), account.availableDeposit)
        assertEquals(TransactionType.SETTLEMENT, ledger.transactionType)
        assertEquals(BigDecimal("50000.0000"), ledger.amount)
    }

    // --- 불변식 시나리오 ---

    @Test
    fun 불변식_deposit은_항상_availableDeposit과_lockedDeposit의_합과_같다() {
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))

        account.lockDeposit(BigDecimal("40000.0000"))
        assertInvariant(account)

        account.unlockDeposit(BigDecimal("10000.0000"))
        assertInvariant(account)

        account.confirmBuy(BigDecimal("30000.0000"))
        assertInvariant(account)

        account.receiveSellProceeds(BigDecimal("30000.0000"))
        assertInvariant(account)

        account.addDeposit(BigDecimal("50000.0000"))
        assertInvariant(account)

        account.withdraw(BigDecimal("20000.0000"))
        assertInvariant(account)
    }

    private fun assertInvariant(account: Account) {
        assertEquals(
            0,
            account.deposit.compareTo(account.availableDeposit.add(account.lockedDeposit)),
            "불변식 위반: deposit != availableDeposit + lockedDeposit"
        )
    }

    private fun assertZero(value: BigDecimal) =
        assertEquals(0, value.compareTo(BigDecimal.ZERO), "Expected zero but was $value")

    private fun createAccount(initialDeposit: BigDecimal): Account {
        return Account.create(
            accountName = "test-account",
            accountType = AccountType.STOCK,
            tradingMode = TradingMode.LOCAL,
            initialDeposit = initialDeposit
        )
    }
}
