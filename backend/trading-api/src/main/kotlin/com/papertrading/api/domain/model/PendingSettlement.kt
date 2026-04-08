package com.papertrading.api.domain.model

import com.papertrading.api.domain.enums.SettlementStatus
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
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "pending_settlements")
class PendingSettlement(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    var account: Account? = null,

    @Column(name = "order_id", nullable = false)
    var orderId: Long? = null,

    @Column(name = "settlement_date", nullable = false)
    var settlementDate: LocalDate? = null,

    @Column(name = "amount", nullable = false, precision = 20, scale = 4)
    var amount: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: SettlementStatus = SettlementStatus.PENDING
) : BaseAuditEntity() {
    fun complete() {
        status = SettlementStatus.COMPLETED
    }
}
