package com.papertrading.api.domain.entity.portfolio

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TradingMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TaxSummaryTest {

    @Test
    fun create_필드가_정상_설정된다() {
        val account = createAccount()

        val summary = TaxSummary.create(
            account = account,
            taxYear = 2024,
            totalRealizedPnl = BigDecimal("1000000.0000"),
            taxablePnl = BigDecimal("800000.0000"),
            estimatedTax = BigDecimal("176000.0000")
        )

        assertEquals(account, summary.account)
        assertEquals(2024, summary.taxYear)
        assertEquals(BigDecimal("1000000.0000"), summary.totalRealizedPnl)
        assertEquals(BigDecimal("800000.0000"), summary.taxablePnl)
        assertEquals(BigDecimal("176000.0000"), summary.estimatedTax)
    }

    @Test
    fun recalculate_값을_재계산한다() {
        val summary = createSummary()

        summary.recalculate(
            totalRealizedPnl = BigDecimal("2000000.0000"),
            taxablePnl = BigDecimal("1600000.0000"),
            estimatedTax = BigDecimal("352000.0000")
        )

        assertEquals(BigDecimal("2000000.0000"), summary.totalRealizedPnl)
        assertEquals(BigDecimal("1600000.0000"), summary.taxablePnl)
        assertEquals(BigDecimal("352000.0000"), summary.estimatedTax)
    }

    @Test
    fun recalculate_예상세금이_0이면_정상_처리된다() {
        val summary = createSummary()

        summary.recalculate(
            totalRealizedPnl = BigDecimal("-500000.0000"),
            taxablePnl = BigDecimal.ZERO,
            estimatedTax = BigDecimal.ZERO
        )

        assertEquals(BigDecimal.ZERO, summary.estimatedTax)
    }

    @Test
    fun recalculate_예상세금이_음수면_예외를_던진다() {
        val summary = createSummary()

        assertThrows(IllegalArgumentException::class.java) {
            summary.recalculate(
                totalRealizedPnl = BigDecimal("1000000.0000"),
                taxablePnl = BigDecimal("800000.0000"),
                estimatedTax = BigDecimal("-1.0000")
            )
        }
    }

    @Test
    fun recalculate_실패해도_기존_값이_유지된다() {
        val summary = createSummary(estimatedTax = BigDecimal("176000.0000"))

        assertThrows(IllegalArgumentException::class.java) {
            summary.recalculate(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal("-1"))
        }

        assertEquals(BigDecimal("176000.0000"), summary.estimatedTax)
    }

    private fun createAccount(): Account = Account.create(
        accountName = "test-account",
        accountType = AccountType.STOCK,
        tradingMode = TradingMode.LOCAL,
        initialDeposit = BigDecimal("100000.0000")
    )

    private fun createSummary(
        estimatedTax: BigDecimal = BigDecimal("176000.0000")
    ): TaxSummary = TaxSummary.create(
        account = createAccount(),
        taxYear = 2024,
        totalRealizedPnl = BigDecimal("1000000.0000"),
        taxablePnl = BigDecimal("800000.0000"),
        estimatedTax = estimatedTax
    )
}
