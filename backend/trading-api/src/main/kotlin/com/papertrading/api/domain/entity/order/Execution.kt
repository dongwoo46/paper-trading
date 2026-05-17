package com.papertrading.api.domain.entity.order

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.base.BaseTimeEntity
import com.papertrading.api.domain.enums.TradingMode
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
import java.time.Instant

/**
 * 체결 (Order 1건에 1~N개, 불변 레코드)
 * 주문이 실제로 거래소(또는 LOCAL 체결 엔진)에서 체결된 결과 기록.
 * external_execution_id UNIQUE — 외부 체결 ID 중복 수신 방지.
 * krwExecutedPrice: currency=KRW 이면 executedPrice와 동일, 외화일 때 fxRate 환산값.
 */
@Entity
@Table(
    name = "executions",
    uniqueConstraints = [UniqueConstraint(name = "uk_executions_external_id", columnNames = ["external_execution_id"])]
)
class Execution protected constructor() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
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

    @Column(name = "executed_quantity", nullable = false, precision = 20, scale = 8)
    lateinit var executedQuantity: BigDecimal
        private set

    @Column(name = "executed_price", nullable = false, precision = 20, scale = 4)
    lateinit var executedPrice: BigDecimal
        private set

    @Column(name = "fee", nullable = false, precision = 20, scale = 4)
    var fee: BigDecimal = BigDecimal.ZERO
        private set

    @Column(name = "currency", nullable = false, length = 3)
    var currency: String = "KRW"
        private set

    @Column(name = "fx_rate", precision = 15, scale = 6)
    var fxRate: BigDecimal? = null
        private set

    @Column(name = "krw_executed_price", nullable = false, precision = 20, scale = 4)
    lateinit var krwExecutedPrice: BigDecimal
        private set

    @Column(name = "external_execution_id", nullable = false, length = 100)
    lateinit var externalExecutionId: String
        private set

    @Column(name = "executed_at", nullable = false)
    lateinit var executedAt: Instant
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_mode", nullable = false, length = 20)
    lateinit var executionMode: TradingMode
        private set

    /** 체결 총 원가 (체결금액 + 수수료) */
    fun totalCost(): BigDecimal = executedQuantity.multiply(executedPrice).add(fee)

    /** 원화 환산 총 원가 (원화 체결금액 + 수수료) */
    fun totalKrwCost(): BigDecimal = executedQuantity.multiply(krwExecutedPrice).add(fee)

    companion object {
        fun create(
            order: Order,
            account: Account,
            ticker: String,
            executedQuantity: BigDecimal,
            executedPrice: BigDecimal,
            krwExecutedPrice: BigDecimal,
            externalExecutionId: String,
            executedAt: Instant,
            executionMode: TradingMode = account.tradingMode,
            fee: BigDecimal = BigDecimal.ZERO,
            currency: String = "KRW",
            fxRate: BigDecimal? = null
        ): Execution {
            require(ticker.isNotBlank()) { "종목코드는 비어 있을 수 없습니다." }
            require(executedQuantity > BigDecimal.ZERO) { "체결 수량은 0보다 커야 합니다." }
            require(executedPrice > BigDecimal.ZERO) { "체결가는 0보다 커야 합니다." }
            require(krwExecutedPrice > BigDecimal.ZERO) { "원화 체결가는 0보다 커야 합니다." }
            require(fee >= BigDecimal.ZERO) { "수수료는 0 이상이어야 합니다." }
            require(externalExecutionId.isNotBlank()) { "외부 체결번호는 비어 있을 수 없습니다." }

            return Execution().apply {
                this.order = order
                this.account = account
                this.ticker = ticker.uppercase()
                this.executedQuantity = executedQuantity
                this.executedPrice = executedPrice
                this.krwExecutedPrice = krwExecutedPrice
                this.externalExecutionId = externalExecutionId
                this.executedAt = executedAt
                this.executionMode = executionMode
                this.fee = fee
                this.currency = currency.uppercase()
                this.fxRate = fxRate
            }
        }
    }
}
