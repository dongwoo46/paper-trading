package com.papertrading.api.domain.entity.feepolicy

import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.entity.base.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

/**
 * 수수료 정책
 * 거래 모드(LOCAL·KIS_PAPER·KIS_LIVE)와 시장 유형(KOSPI·KOSDAQ·NASDAQ·NYSE·CRYPTO)별 수수료율·최소수수료 정의.
 * effectiveFrom~effectiveUntil 기간 이력 관리 — 정책 변경 시 신규 행 추가, 기존 행 보존.
 */
@Entity
@Table(name = "fee_policies")
class FeePolicy protected constructor() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "trading_mode", nullable = false, length = 20)
    lateinit var tradingMode: TradingMode
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", nullable = false, length = 20)
    lateinit var marketType: MarketType
        private set

    @Column(name = "fee_rate", nullable = false, precision = 10, scale = 6)
    lateinit var feeRate: BigDecimal
        private set

    @Column(name = "min_fee", nullable = false, precision = 20, scale = 4)
    lateinit var minFee: BigDecimal
        private set

    @Column(name = "effective_from", nullable = false)
    lateinit var effectiveFrom: Instant
        private set

    @Column(name = "effective_until")
    var effectiveUntil: Instant? = null
        private set

    /** 정책을 만료 처리한다. 이미 만료된 정책에는 호출 불가. */
    fun expire(at: Instant) {
        check(effectiveUntil == null) { "이미 만료된 수수료 정책입니다." }
        require(at > effectiveFrom) { "만료 일시는 시작 일시 이후여야 합니다." }
        effectiveUntil = at
    }

    /** 주어진 시각에 이 정책이 유효한지 확인한다. */
    fun isActiveAt(at: Instant): Boolean =
        !at.isBefore(effectiveFrom) && (effectiveUntil == null || at.isBefore(effectiveUntil!!))

    fun isCurrent(): Boolean = isActiveAt(Instant.now())

    /**
     * 거래 금액에 대한 수수료를 계산한다.
     * max(tradeAmount × feeRate, minFee), scale=4 올림.
     */
    fun calculateFee(tradeAmount: BigDecimal): BigDecimal {
        require(tradeAmount >= BigDecimal.ZERO) { "거래 금액은 0 이상이어야 합니다." }
        val calculated = tradeAmount.multiply(feeRate).setScale(4, RoundingMode.UP)
        return calculated.max(minFee)
    }

    companion object {
        fun create(
            tradingMode: TradingMode,
            marketType: MarketType,
            feeRate: BigDecimal,
            minFee: BigDecimal,
            effectiveFrom: Instant,
            effectiveUntil: Instant? = null
        ): FeePolicy {
            require(feeRate >= BigDecimal.ZERO) { "수수료율은 0 이상이어야 합니다." }
            require(feeRate <= BigDecimal.ONE) { "수수료율은 1 이하여야 합니다." }
            require(minFee >= BigDecimal.ZERO) { "최소 수수료는 0 이상이어야 합니다." }
            if (effectiveUntil != null) {
                require(effectiveUntil > effectiveFrom) { "만료 일시는 시작 일시 이후여야 합니다." }
            }

            return FeePolicy().apply {
                this.tradingMode = tradingMode
                this.marketType = marketType
                this.feeRate = feeRate
                this.minFee = minFee
                this.effectiveFrom = effectiveFrom
                this.effectiveUntil = effectiveUntil
            }
        }
    }
}