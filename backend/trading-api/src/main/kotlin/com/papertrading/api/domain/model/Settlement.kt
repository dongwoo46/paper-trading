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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
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
class Settlement(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    var account: Account? = null,

    @Column(name = "ticker", nullable = false, length = 20)
    var ticker: String? = null,

    @Column(name = "realized_pnl", nullable = false, precision = 20, scale = 4)
    var realizedPnl: BigDecimal = BigDecimal.ZERO,

    @Column(name = "fee", nullable = false, precision = 20, scale = 4)
    var fee: BigDecimal = BigDecimal.ZERO,

    @Column(name = "tax", nullable = false, precision = 20, scale = 4)
    var tax: BigDecimal = BigDecimal.ZERO,

    @Column(name = "net_pnl", nullable = false, precision = 20, scale = 4)
    var netPnl: BigDecimal = BigDecimal.ZERO,

    @Column(name = "currency", nullable = false, length = 3)
    var currency: String = "KRW",

    @Column(name = "fx_rate_at_settlement", precision = 15, scale = 6)
    var fxRateAtSettlement: BigDecimal? = null,

    @Column(name = "krw_net_pnl", nullable = false, precision = 20, scale = 4)
    var krwNetPnl: BigDecimal = BigDecimal.ZERO,

    @Column(name = "settled_at", nullable = false)
    var settledAt: Instant? = null
) : BaseTimeEntity()
