package com.papertrading.api.domain.model

import com.papertrading.api.domain.model.base.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
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

@Entity
@Table(
    name = "executions",
    uniqueConstraints = [UniqueConstraint(name = "uk_executions_external_id", columnNames = ["external_execution_id"])]
)
class Execution(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    var account: Account? = null,

    @Column(name = "ticker", nullable = false, length = 20)
    var ticker: String? = null,

    @Column(name = "executed_quantity", nullable = false, precision = 20, scale = 8)
    var executedQuantity: BigDecimal = BigDecimal.ZERO,

    @Column(name = "executed_price", nullable = false, precision = 20, scale = 4)
    var executedPrice: BigDecimal = BigDecimal.ZERO,

    @Column(name = "fee", nullable = false, precision = 20, scale = 4)
    var fee: BigDecimal = BigDecimal.ZERO,

    @Column(name = "currency", nullable = false, length = 3)
    var currency: String = "KRW",

    @Column(name = "fx_rate", precision = 15, scale = 6)
    var fxRate: BigDecimal? = null,

    @Column(name = "krw_executed_price", nullable = false, precision = 20, scale = 4)
    var krwExecutedPrice: BigDecimal = BigDecimal.ZERO,

    @Column(name = "external_execution_id", nullable = false, length = 100)
    var externalExecutionId: String? = null,

    @Column(name = "executed_at", nullable = false)
    var executedAt: Instant? = null
) : BaseTimeEntity()
