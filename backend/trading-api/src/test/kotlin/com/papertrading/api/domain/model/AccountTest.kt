package com.papertrading.api.domain.model

import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TradingMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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
        assertEquals(BigDecimal.ZERO, account.lockedDeposit)
    }

    @Test
    fun 입금하면_예수금과_가용예수금이_동시에_증가한다() {
        // given
        val account = createAccount(initialDeposit = BigDecimal("100000.0000"))

        // when
        account.deposit(BigDecimal("50000.0000"))

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
            account.deposit(BigDecimal.ZERO)
        }
        assertThrows(IllegalArgumentException::class.java) {
            account.deposit(BigDecimal("-1"))
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

    private fun createAccount(initialDeposit: BigDecimal): Account {
        return Account.create(
            accountName = "test-account",
            accountType = AccountType.STOCK,
            tradingMode = TradingMode.LOCAL,
            initialDeposit = initialDeposit
        )
    }
}
