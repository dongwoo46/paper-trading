package com.papertrading.api.domain.model

import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.PriceSource
import com.papertrading.api.domain.model.base.BaseAuditEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

/**
 * 포지션 (보유 종목)
 * 계좌별·종목별 보유 수량과 평균 매수가를 관리
 * quantity = 총 보유 수량, lockedQuantity = 매도 주문 잠금 수량, orderableQuantity = 주문 가능 수량
 * 체결 시 applyBuy/applySell로 수량·단가 갱신, 시세 수신 시 updatePrice로 평가손익 갱신
 * 데드락 방지: 항상 Account 락 획득 후 Position 락 획득 순서 준수
 */
@Entity
@Table(
    name = "positions",
    uniqueConstraints = [UniqueConstraint(name = "uk_positions_account_ticker", columnNames = ["account_id", "ticker"])]
)
class Position(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    var account: Account? = null,

    @Column(name = "ticker", nullable = false, length = 20)
    var ticker: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", nullable = false, length = 20)
    var marketType: MarketType? = null,

    @Column(name = "quantity", nullable = false, precision = 20, scale = 8)
    var quantity: BigDecimal = BigDecimal.ZERO,

    @Column(name = "locked_quantity", nullable = false, precision = 20, scale = 8)
    var lockedQuantity: BigDecimal = BigDecimal.ZERO,

    @Column(name = "orderable_quantity", nullable = false, precision = 20, scale = 8)
    var orderableQuantity: BigDecimal = BigDecimal.ZERO,

    @Column(name = "avg_buy_price", nullable = false, precision = 20, scale = 4)
    var avgBuyPrice: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_buy_amount", nullable = false, precision = 20, scale = 4)
    var totalBuyAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "current_price", precision = 20, scale = 4)
    var currentPrice: BigDecimal? = null,

    @Column(name = "evaluation_amount", precision = 20, scale = 4)
    var evaluationAmount: BigDecimal? = null,

    @Column(name = "unrealized_pnl", precision = 20, scale = 4)
    var unrealizedPnl: BigDecimal? = null,

    @Column(name = "return_rate", precision = 10, scale = 4)
    var returnRate: BigDecimal? = null,

    @Column(name = "price_updated_at")
    var priceUpdatedAt: Instant? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "price_source", length = 20)
    var priceSource: PriceSource = PriceSource.UNKNOWN
) : BaseAuditEntity() {

    fun applyBuy(executedQty: BigDecimal, executedPrice: BigDecimal) {
        val newTotalBuyAmount = totalBuyAmount.add(executedQty.multiply(executedPrice))
        val newQuantity = quantity.add(executedQty)
        avgBuyPrice = newTotalBuyAmount.divide(newQuantity, 4, RoundingMode.HALF_UP)
        totalBuyAmount = newTotalBuyAmount
        quantity = newQuantity
        orderableQuantity = quantity.subtract(lockedQuantity)
    }

    fun applySell(executedQty: BigDecimal) {
        quantity = quantity.subtract(executedQty)
        lockedQuantity = lockedQuantity.subtract(executedQty)
        orderableQuantity = quantity.subtract(lockedQuantity)
        totalBuyAmount = avgBuyPrice.multiply(quantity)
    }

    fun updatePrice(price: BigDecimal, source: PriceSource) {
        currentPrice = price
        evaluationAmount = price.multiply(quantity)
        unrealizedPnl = evaluationAmount?.subtract(totalBuyAmount)
        if (avgBuyPrice > BigDecimal.ZERO) {
            returnRate = price.subtract(avgBuyPrice).divide(avgBuyPrice, 4, RoundingMode.HALF_UP)
        }
        priceUpdatedAt = Instant.now()
        priceSource = source
    }
}
