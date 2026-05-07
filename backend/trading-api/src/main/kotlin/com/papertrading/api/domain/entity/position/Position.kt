package com.papertrading.api.domain.entity.position
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.PriceSource
import com.papertrading.api.domain.entity.base.BaseAuditEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Check
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
    uniqueConstraints = [UniqueConstraint(name = "uk_positions_account_ticker", columnNames = ["account_id", "ticker"])],
    indexes = [
        Index(name = "idx_positions_account_qty", columnList = "account_id, quantity"),
        Index(name = "idx_positions_ticker_qty", columnList = "ticker, quantity"),
    ]
)
@Check(constraints = "quantity >= 0 AND locked_quantity >= 0 AND orderable_quantity >= 0 AND quantity >= locked_quantity")
class Position protected constructor() : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account
        private set

    @Column(name = "ticker", nullable = false, length = 20)
    lateinit var ticker: String
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", nullable = false, length = 20)
    lateinit var marketType: MarketType
        private set

    @Column(name = "quantity", nullable = false, precision = 20, scale = 8)
    var quantity: BigDecimal = BigDecimal.ZERO
        private set

    @Column(name = "locked_quantity", nullable = false, precision = 20, scale = 8)
    var lockedQuantity: BigDecimal = BigDecimal.ZERO
        private set

    @Column(name = "orderable_quantity", nullable = false, precision = 20, scale = 8)
    var orderableQuantity: BigDecimal = BigDecimal.ZERO
        private set

    @Column(name = "avg_buy_price", nullable = false, precision = 20, scale = 4)
    var avgBuyPrice: BigDecimal = BigDecimal.ZERO
        private set

    @Column(name = "total_buy_amount", nullable = false, precision = 20, scale = 4)
    var totalBuyAmount: BigDecimal = BigDecimal.ZERO
        private set

    @Column(name = "current_price", precision = 20, scale = 4)
    var currentPrice: BigDecimal? = null
        private set

    @Column(name = "evaluation_amount", precision = 20, scale = 4)
    var evaluationAmount: BigDecimal? = null
        private set

    @Column(name = "unrealized_pnl", precision = 20, scale = 4)
    var unrealizedPnl: BigDecimal? = null
        private set

    @Column(name = "return_rate", precision = 10, scale = 4)
    var returnRate: BigDecimal? = null
        private set

    @Column(name = "price_updated_at")
    var priceUpdatedAt: Instant? = null
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "price_source", length = 20)
    var priceSource: PriceSource = PriceSource.UNKNOWN
        private set

    companion object {
        fun create(
            account: Account,
            ticker: String,
            marketType: MarketType,
        ): Position {
            require(ticker.isNotBlank()) { "ticker는 비어 있을 수 없습니다." }
            return Position().apply {
                this.account = account
                this.ticker = ticker.trim().uppercase()
                this.marketType = marketType
            }
        }

        fun createWithHolding(
            account: Account,
            ticker: String,
            marketType: MarketType,
            quantity: BigDecimal,
            avgBuyPrice: BigDecimal,
            lockedQuantity: BigDecimal = BigDecimal.ZERO,
            currentPrice: BigDecimal? = null,
            priceSource: PriceSource = PriceSource.UNKNOWN,
        ): Position {
            require(quantity >= BigDecimal.ZERO) { "보유 수량은 0 이상이어야 합니다." }
            require(avgBuyPrice >= BigDecimal.ZERO) { "평균 매수가는 0 이상이어야 합니다." }
            require(lockedQuantity >= BigDecimal.ZERO) { "잠금 수량은 0 이상이어야 합니다." }
            require(quantity >= lockedQuantity) { "잠금 수량은 보유 수량을 초과할 수 없습니다." }

            val position = create(account, ticker, marketType).apply {
                this.quantity = quantity
                this.lockedQuantity = lockedQuantity
                this.orderableQuantity = quantity.subtract(lockedQuantity)
                this.avgBuyPrice = avgBuyPrice
                this.totalBuyAmount = avgBuyPrice.multiply(quantity)
                this.priceSource = priceSource
            }

            if (currentPrice != null) {
                position.updatePrice(currentPrice, priceSource)
            }
            return position
        }
    }

    fun applyBuy(executedQty: BigDecimal, executedPrice: BigDecimal) {
        require(executedQty > BigDecimal.ZERO) { "매수 체결 수량은 0보다 커야 합니다." }
        require(executedPrice > BigDecimal.ZERO) { "매수 체결 가격은 0보다 커야 합니다." }
        val newTotalBuyAmount = totalBuyAmount.add(executedQty.multiply(executedPrice))
        val newQuantity = quantity.add(executedQty)
        avgBuyPrice = newTotalBuyAmount.divide(newQuantity, 4, RoundingMode.HALF_UP)
        totalBuyAmount = newTotalBuyAmount
        quantity = newQuantity
        orderableQuantity = quantity.subtract(lockedQuantity)
    }

    fun applySell(executedQty: BigDecimal) {
        require(executedQty > BigDecimal.ZERO) { "매도 체결 수량은 0보다 커야 합니다." }
        require(quantity >= executedQty) { "보유 수량이 부족합니다." }
        require(lockedQuantity >= executedQty) { "잠금 수량이 부족합니다." }
        quantity = quantity.subtract(executedQty)
        lockedQuantity = lockedQuantity.subtract(executedQty)
        orderableQuantity = quantity.subtract(lockedQuantity)
        totalBuyAmount = avgBuyPrice.multiply(quantity)
    }

    fun lockQuantity(qty: BigDecimal) {
        require(qty > BigDecimal.ZERO) { "잠금 수량은 0보다 커야 합니다." }
        require(orderableQuantity >= qty) { "주문 가능 수량이 부족합니다. available=$orderableQuantity, requested=$qty" }
        lockedQuantity = lockedQuantity.add(qty)
        orderableQuantity = quantity.subtract(lockedQuantity)
    }

    fun unlockQuantity(qty: BigDecimal) {
        require(qty > BigDecimal.ZERO) { "해제 수량은 0보다 커야 합니다." }
        require(lockedQuantity >= qty) { "잠금 수량이 부족합니다. locked=$lockedQuantity, requested=$qty" }
        lockedQuantity = lockedQuantity.subtract(qty)
        orderableQuantity = quantity.subtract(lockedQuantity)
    }

    fun updatePrice(price: BigDecimal, source: PriceSource) {
        require(price >= BigDecimal.ZERO) { "현재가는 0 이상이어야 합니다." }
        currentPrice = price
        evaluationAmount = price.multiply(quantity)
        unrealizedPnl = evaluationAmount?.subtract(totalBuyAmount)
        returnRate = if (avgBuyPrice > BigDecimal.ZERO) {
            price.subtract(avgBuyPrice).divide(avgBuyPrice, 4, RoundingMode.HALF_UP)
        } else {
            null
        }
        priceUpdatedAt = Instant.now()
        priceSource = source
    }
}
