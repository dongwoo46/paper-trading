package com.papertrading.api.application.portfolio.tax

import com.papertrading.api.common.exception.UnsupportedCurrencyException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class TaxSummaryCalculatorTest {

    private val calculator = TaxSummaryCalculator()

    @Test
    fun `compute scale 4 and expected tax`() {
        val result = calculator.compute(
            TaxSettlementAggregate(
                totalRealizedPnl = BigDecimal("1000.1"),
                totalFee = BigDecimal("10"),
                totalTax = BigDecimal("5.55"),
                currency = "KRW",
            )
        )

        assertEquals(4, result.totalRealizedPnl.scale())
        assertEquals(4, result.taxablePnl.scale())
        assertEquals(4, result.estimatedTax.scale())
        assertEquals(BigDecimal("984.5500"), result.taxablePnl)
        assertEquals(BigDecimal("5.5500"), result.estimatedTax)
    }

    @Test
    fun `compute taxable less than zero then clamp to zero`() {
        val result = calculator.compute(
            TaxSettlementAggregate(
                totalRealizedPnl = BigDecimal("10"),
                totalFee = BigDecimal("20"),
                totalTax = BigDecimal("5"),
                currency = "KRW",
            )
        )

        assertEquals(BigDecimal("0.0000"), result.taxablePnl)
        assertEquals(BigDecimal("5.0000"), result.estimatedTax)
    }

    @Test
    fun `non KRW currency throws`() {
        assertThrows<UnsupportedCurrencyException> {
            calculator.compute(
                TaxSettlementAggregate(
                    totalRealizedPnl = BigDecimal.ZERO,
                    totalFee = BigDecimal.ZERO,
                    totalTax = BigDecimal.ZERO,
                    currency = "USD",
                )
            )
        }
    }
}
