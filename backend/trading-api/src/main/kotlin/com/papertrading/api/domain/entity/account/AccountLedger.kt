package com.papertrading.api.domain.entity.account

import com.papertrading.api.domain.entity.base.BaseTimeEntity
import com.papertrading.api.domain.enums.TransactionType
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

/**
 * 계좌 원장
 * 입금·출금·매수잠금 등 계좌의 모든 자금 이동을 불변 이력으로 기록
 * idempotencyKey 유니크 제약으로 동일 요청 중복 기록 방지
 */
@Entity
@Table(
    name = "account_ledger",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_account_ledger_account_id_idempotency",
            columnNames = ["account_id", "idempotency_key"]
        )
    ]
)
class AccountLedger protected constructor() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    lateinit var transactionType: TransactionType
        private set

    @Column(name = "amount", nullable = false, precision = 20, scale = 4)
    lateinit var amount: BigDecimal
        private set

    @Column(name = "balance_after", nullable = false, precision = 20, scale = 4)
    lateinit var balanceAfter: BigDecimal
        private set

    @Column(name = "ref_order_id")
    var refOrderId: Long? = null
        private set

    @Column(name = "ref_execution_id")
    var refExecutionId: Long? = null
        private set

    @Column(name = "description", length = 500)
    var description: String? = null
        private set

    @Column(name = "idempotency_key", nullable = false, length = 100)
    lateinit var idempotencyKey: String
        private set

    companion object {
        internal fun create(
            account: Account,
            transactionType: TransactionType,
            amount: BigDecimal,
            balanceAfter: BigDecimal,
            idempotencyKey: String,
            refOrderId: Long? = null,
            refExecutionId: Long? = null,
            description: String? = null
        ): AccountLedger = AccountLedger().apply {
            this.account = account
            this.transactionType = transactionType
            this.amount = amount
            this.balanceAfter = balanceAfter
            this.idempotencyKey = idempotencyKey
            this.refOrderId = refOrderId
            this.refExecutionId = refExecutionId
            this.description = description
        }
    }
}
