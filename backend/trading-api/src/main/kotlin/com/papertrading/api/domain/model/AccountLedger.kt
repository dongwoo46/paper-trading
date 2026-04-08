package com.papertrading.api.domain.model

import com.papertrading.api.domain.enums.TransactionType
import com.papertrading.api.domain.model.base.BaseTimeEntity
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

@Entity
@Table(
    name = "account_ledger",
    uniqueConstraints = [UniqueConstraint(name = "uk_account_ledger_idempotency", columnNames = ["idempotency_key"])]
)
class AccountLedger(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    var account: Account? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    var transactionType: TransactionType? = null,

    @Column(name = "amount", nullable = false, precision = 20, scale = 4)
    var amount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "balance_after", nullable = false, precision = 20, scale = 4)
    var balanceAfter: BigDecimal = BigDecimal.ZERO,

    @Column(name = "ref_order_id")
    var refOrderId: Long? = null,

    @Column(name = "ref_execution_id")
    var refExecutionId: Long? = null,

    @Column(name = "description", length = 500)
    var description: String? = null,

    @Column(name = "idempotency_key", nullable = false, length = 100)
    var idempotencyKey: String? = null
) : BaseTimeEntity()
