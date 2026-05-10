package com.papertrading.api.domain.entity.feepolicy

import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.TradingMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class FeePolicyTest {

    // --- create ---

    @Test
    fun create_정상_생성되면_필드값이_올바르게_설정된다() {
        val from = Instant.parse("2024-01-01T00:00:00Z")

        val policy = createPolicy(effectiveFrom = from)

        assertEquals(TradingMode.LOCAL, policy.tradingMode)
        assertEquals(MarketType.KOSPI, policy.marketType)
        assertEquals(BigDecimal("0.001500"), policy.feeRate)
        assertEquals(BigDecimal("0.0000"), policy.minFee)
        assertEquals(from, policy.effectiveFrom)
    }

    @Test
    fun create_effectiveUntil이_null이면_오픈엔드_정책으로_생성된다() {
        val policy = createPolicy(effectiveUntil = null)

        assertTrue(policy.effectiveUntil == null)
    }

    @Test
    fun create_feeRate가_음수이면_예외를_던진다() {
        assertThrows(IllegalArgumentException::class.java) {
            createPolicy(feeRate = BigDecimal("-0.001"))
        }
    }

    @Test
    fun create_feeRate가_1_초과면_예외를_던진다() {
        assertThrows(IllegalArgumentException::class.java) {
            createPolicy(feeRate = BigDecimal("1.0001"))
        }
    }

    @Test
    fun create_feeRate_경계값_0과_1은_유효하다() {
        val p0 = createPolicy(feeRate = BigDecimal.ZERO)
        val p1 = createPolicy(feeRate = BigDecimal.ONE)

        assertEquals(0, p0.feeRate.compareTo(BigDecimal.ZERO))
        assertEquals(0, p1.feeRate.compareTo(BigDecimal.ONE))
    }

    @Test
    fun create_minFee가_음수이면_예외를_던진다() {
        assertThrows(IllegalArgumentException::class.java) {
            createPolicy(minFee = BigDecimal("-1"))
        }
    }

    @Test
    fun create_effectiveUntil이_effectiveFrom보다_이전이면_예외를_던진다() {
        val from = Instant.parse("2024-06-01T00:00:00Z")
        val until = Instant.parse("2024-01-01T00:00:00Z")

        assertThrows(IllegalArgumentException::class.java) {
            createPolicy(effectiveFrom = from, effectiveUntil = until)
        }
    }

    @Test
    fun create_effectiveUntil이_effectiveFrom과_같으면_예외를_던진다() {
        val from = Instant.parse("2024-01-01T00:00:00Z")

        assertThrows(IllegalArgumentException::class.java) {
            createPolicy(effectiveFrom = from, effectiveUntil = from)
        }
    }

    // --- isActiveAt ---

    @Test
    fun isActiveAt_시작일시와_같으면_활성으로_판단한다() {
        val from = Instant.parse("2024-01-01T00:00:00Z")
        val policy = createPolicy(effectiveFrom = from, effectiveUntil = null)

        assertTrue(policy.isActiveAt(from))
    }

    @Test
    fun isActiveAt_시작일시_이전이면_비활성으로_판단한다() {
        val from = Instant.parse("2024-01-01T00:00:00Z")
        val policy = createPolicy(effectiveFrom = from, effectiveUntil = null)

        assertFalse(policy.isActiveAt(from.minusSeconds(1)))
    }

    @Test
    fun isActiveAt_만료일시_이후이면_비활성으로_판단한다() {
        val from = Instant.parse("2024-01-01T00:00:00Z")
        val until = Instant.parse("2024-12-31T00:00:00Z")
        val policy = createPolicy(effectiveFrom = from, effectiveUntil = until)

        assertFalse(policy.isActiveAt(until))
        assertFalse(policy.isActiveAt(until.plusSeconds(1)))
    }

    @Test
    fun isActiveAt_기간_내부이면_활성으로_판단한다() {
        val from = Instant.parse("2024-01-01T00:00:00Z")
        val until = Instant.parse("2024-12-31T00:00:00Z")
        val policy = createPolicy(effectiveFrom = from, effectiveUntil = until)

        assertTrue(policy.isActiveAt(Instant.parse("2024-06-15T00:00:00Z")))
    }

    @Test
    fun isActiveAt_effectiveUntil이_null이면_시작일시_이후는_항상_활성이다() {
        val from = Instant.parse("2020-01-01T00:00:00Z")
        val policy = createPolicy(effectiveFrom = from, effectiveUntil = null)

        assertTrue(policy.isActiveAt(Instant.parse("2099-12-31T23:59:59Z")))
    }

    // --- expire ---

    @Test
    fun expire_만료일시를_설정하면_effectiveUntil이_갱신된다() {
        val from = Instant.parse("2024-01-01T00:00:00Z")
        val expireAt = Instant.parse("2024-06-01T00:00:00Z")
        val policy = createPolicy(effectiveFrom = from, effectiveUntil = null)

        policy.expire(expireAt)

        assertEquals(expireAt, policy.effectiveUntil)
        assertFalse(policy.isActiveAt(expireAt))
    }

    @Test
    fun expire_이미_만료된_정책에_호출하면_예외를_던진다() {
        val from = Instant.parse("2024-01-01T00:00:00Z")
        val until = Instant.parse("2024-06-01T00:00:00Z")
        val policy = createPolicy(effectiveFrom = from, effectiveUntil = until)

        assertThrows(IllegalStateException::class.java) {
            policy.expire(Instant.parse("2024-09-01T00:00:00Z"))
        }
    }

    @Test
    fun expire_만료일시가_시작일시보다_이전이면_예외를_던진다() {
        val from = Instant.parse("2024-06-01T00:00:00Z")
        val policy = createPolicy(effectiveFrom = from, effectiveUntil = null)

        assertThrows(IllegalArgumentException::class.java) {
            policy.expire(Instant.parse("2024-01-01T00:00:00Z"))
        }
    }

    // --- calculateFee ---

    @Test
    fun calculateFee_계산값이_minFee보다_크면_계산값을_반환한다() {
        // feeRate=0.001500, tradeAmount=1000000 → 1500.0000, minFee=500
        val policy = createPolicy(feeRate = BigDecimal("0.001500"), minFee = BigDecimal("500.0000"))

        val fee = policy.calculateFee(BigDecimal("1000000"))

        assertEquals(0, fee.compareTo(BigDecimal("1500.0000")))
    }

    @Test
    fun calculateFee_계산값이_minFee보다_작으면_minFee를_반환한다() {
        // feeRate=0.001500, tradeAmount=1000 → 1.5000, minFee=500
        val policy = createPolicy(feeRate = BigDecimal("0.001500"), minFee = BigDecimal("500.0000"))

        val fee = policy.calculateFee(BigDecimal("1000"))

        assertEquals(0, fee.compareTo(BigDecimal("500.0000")))
    }

    @Test
    fun calculateFee_거래금액이_0이면_minFee를_반환한다() {
        val policy = createPolicy(feeRate = BigDecimal("0.001500"), minFee = BigDecimal("500.0000"))

        val fee = policy.calculateFee(BigDecimal.ZERO)

        assertEquals(0, fee.compareTo(BigDecimal("500.0000")))
    }

    @Test
    fun calculateFee_minFee가_0이고_거래금액도_0이면_0을_반환한다() {
        val policy = createPolicy(feeRate = BigDecimal("0.001500"), minFee = BigDecimal.ZERO)

        val fee = policy.calculateFee(BigDecimal.ZERO)

        assertEquals(0, fee.compareTo(BigDecimal.ZERO))
    }

    @Test
    fun calculateFee_소수점은_4자리_올림으로_반환한다() {
        // feeRate=0.001, tradeAmount=3 → 0.003 → scale=4,UP → 0.0030
        val policy = createPolicy(feeRate = BigDecimal("0.001000"), minFee = BigDecimal.ZERO)

        val fee = policy.calculateFee(BigDecimal("3"))

        assertEquals(4, fee.scale())
        assertEquals(0, fee.compareTo(BigDecimal("0.003").setScale(4)))
    }

    @Test
    fun calculateFee_계산값이_minFee와_정확히_같으면_그대로_반환한다() {
        // feeRate=0.001000, tradeAmount=500000 → 500.0000, minFee=500.0000
        val policy = createPolicy(feeRate = BigDecimal("0.001000"), minFee = BigDecimal("500.0000"))

        val fee = policy.calculateFee(BigDecimal("500000"))

        assertEquals(0, fee.compareTo(BigDecimal("500.0000")))
    }

    @Test
    fun calculateFee_거래금액이_음수이면_예외를_던진다() {
        val policy = createPolicy()

        assertThrows(IllegalArgumentException::class.java) {
            policy.calculateFee(BigDecimal("-1"))
        }
    }

    // --- calculateTax ---

    @Test
    fun calculateTax_KOSPI_세율_0_0018이면_올바른_세금을_계산한다() {
        // grossProceeds=1000000, taxRate=0.0018 → 1800.0000
        val policy = createPolicy(transactionTaxRate = BigDecimal("0.0018"))

        val tax = policy.calculateTax(BigDecimal("1000000"))

        assertEquals(0, tax.compareTo(BigDecimal("1800.0000")))
        assertEquals(4, tax.scale())
    }

    @Test
    fun calculateTax_NASDAQ_세율_0이면_세금이_0이다() {
        val policy = createPolicy(transactionTaxRate = BigDecimal("0.0000"))

        val tax = policy.calculateTax(BigDecimal("1000000"))

        assertEquals(0, tax.compareTo(BigDecimal.ZERO))
    }

    @Test
    fun calculateTax_grossProceeds가_음수이면_예외를_던진다() {
        val policy = createPolicy(transactionTaxRate = BigDecimal("0.0018"))

        assertThrows(IllegalArgumentException::class.java) {
            policy.calculateTax(BigDecimal("-1"))
        }
    }

    @Test
    fun create_transactionTaxRate가_음수이면_예외를_던진다() {
        assertThrows(IllegalArgumentException::class.java) {
            createPolicy(transactionTaxRate = BigDecimal("-0.001"))
        }
    }

    @Test
    fun create_transactionTaxRate가_1_초과이면_예외를_던진다() {
        assertThrows(IllegalArgumentException::class.java) {
            createPolicy(transactionTaxRate = BigDecimal("1.0001"))
        }
    }

    // --- helper ---

    private fun createPolicy(
        tradingMode: TradingMode = TradingMode.LOCAL,
        marketType: MarketType = MarketType.KOSPI,
        feeRate: BigDecimal = BigDecimal("0.001500"),
        minFee: BigDecimal = BigDecimal("0.0000"),
        transactionTaxRate: BigDecimal = BigDecimal("0.0018"),
        effectiveFrom: Instant = Instant.parse("2024-01-01T00:00:00Z"),
        effectiveUntil: Instant? = null
    ): FeePolicy = FeePolicy.create(
        tradingMode = tradingMode,
        marketType = marketType,
        feeRate = feeRate,
        minFee = minFee,
        transactionTaxRate = transactionTaxRate,
        effectiveFrom = effectiveFrom,
        effectiveUntil = effectiveUntil
    )
}