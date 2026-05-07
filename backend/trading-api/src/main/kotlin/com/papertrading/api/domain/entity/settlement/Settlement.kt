package com.papertrading.api.domain.entity.settlement
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.order.Execution

import com.papertrading.api.domain.entity.base.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

/**
 * 정산 (주문 완전 체결 시 1건 생성)
 * FILLED 주문의 실현손익(realizedPnl), 수수료(fee), 세금(tax)을 확정 기록.
 * netPnl = realizedPnl - fee - tax. 다중 통화는 krwNetPnl로 원화 환산.
 * 어떤 Execution들이 포함됐는지는 SettlementExecution 조인 테이블로 추적.
 */
@Entity
@Table(
    name = "settlements",
    uniqueConstraints = [UniqueConstraint(name = "uk_settlements_order", columnNames = ["order_id"])]
)
class Settlement protected constructor() : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    lateinit var order: Order
        private set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account
        private set

    @Column(name = "ticker", nullable = false, length = 20)
    lateinit var ticker: String
        private set

    @Column(name = "realized_pnl", nullable = false, precision = 20, scale = 4)
    var realizedPnl: BigDecimal = BigDecimal.ZERO
        private set

    @Column(name = "fee", nullable = false, precision = 20, scale = 4)
    var fee: BigDecimal = BigDecimal.ZERO
        private set

    @Column(name = "tax", nullable = false, precision = 20, scale = 4)
    var tax: BigDecimal = BigDecimal.ZERO
        private set

    @Column(name = "net_pnl", nullable = false, precision = 20, scale = 4)
    var netPnl: BigDecimal = BigDecimal.ZERO
        private set

    @Column(name = "currency", nullable = false, length = 3)
    var currency: String = "KRW"
        private set

    @Column(name = "fx_rate_at_settlement", precision = 15, scale = 6)
    var fxRateAtSettlement: BigDecimal? = null
        private set

    @Column(name = "krw_net_pnl", nullable = false, precision = 20, scale = 4)
    var krwNetPnl: BigDecimal = BigDecimal.ZERO
        private set

    @Column(name = "settled_at", nullable = false)
    lateinit var settledAt: Instant
        private set

    companion object {
        fun createFromExecution(
            order: Order,
            account: Account,
            execution: Execution,
            tax: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            settledAt: Instant,
            fxRateAtSettlement: BigDecimal? = execution.fxRate,
        ): Settlement {
            val executedPrice = execution.executedPrice
            val executedQty = execution.executedQuantity
            val realizedPnl = executedPrice.multiply(executedQty).setScale(4, RoundingMode.HALF_UP)
            val fee = execution.fee.setScale(4, RoundingMode.HALF_UP)
            val normalizedTax = tax.setScale(4, RoundingMode.HALF_UP)
            val netPnl = realizedPnl.subtract(fee).subtract(normalizedTax).setScale(4, RoundingMode.HALF_UP)
            val ticker = requireNotNull(execution.ticker) { "execution.ticker is null" }
            val currency = execution.currency
            val normalizedCurrency = currency.uppercase()

            val resolvedFxRate = if (normalizedCurrency == "KRW") null else fxRateAtSettlement
            val krwNetPnl = if (normalizedCurrency == "KRW") {
                netPnl
            } else {
                requireNotNull(resolvedFxRate) { "외화 정산은 fxRateAtSettlement가 필요합니다." }
                    .let { rate -> netPnl.multiply(rate).setScale(4, RoundingMode.HALF_UP) }
            }
            require(fee >= BigDecimal.ZERO) { "fee는 0 이상이어야 합니다." }
            require(normalizedTax >= BigDecimal.ZERO) { "tax는 0 이상이어야 합니다." }
            return Settlement().apply {
                this.order = order
                this.account = account
                this.ticker = ticker.trim().uppercase()
                this.realizedPnl = realizedPnl
                this.fee = fee
                this.tax = normalizedTax
                this.netPnl = netPnl
                this.currency = normalizedCurrency
                this.fxRateAtSettlement = resolvedFxRate
                this.krwNetPnl = krwNetPnl
                this.settledAt = settledAt
            }
        }
    }
}
