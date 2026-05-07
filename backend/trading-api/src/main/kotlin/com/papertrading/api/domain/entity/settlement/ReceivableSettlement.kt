package com.papertrading.api.domain.entity.settlement
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.enums.SettlementStatus
import com.papertrading.api.domain.entity.base.BaseAuditEntity
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

/**
 * 정산 미수금(입금 대기) 엔티티.
 * 매도 체결 후 실제 현금이 계좌에 들어오기 전까지의 receivable 상태를 나타낸다.
 * 주식은 T+2 결제 기준으로 settlementDate에 입금 가능해진다.
 * status: PENDING(입금 대기) → COMPLETED(입금 반영 완료)
 */
@Entity
@Table(name = "receivable_settlements")
class ReceivableSettlement protected constructor() : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account
        private set

    @Column(name = "order_id", nullable = false)
    var orderId: Long = 0
        private set

    @Column(name = "settlement_date", nullable = false)
    lateinit var settlementDate: LocalDate
        private set

    @Column(name = "amount", nullable = false, precision = 20, scale = 4)
    var amount: BigDecimal = BigDecimal.ZERO
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: SettlementStatus = SettlementStatus.PENDING
        private set

    fun complete(asOf: LocalDate) {
        check(!asOf.isBefore(settlementDate)) {
            "입금 가능일 이전에는 완료할 수 없습니다. settlementDate=$settlementDate, asOf=$asOf"
        }
        check(status == SettlementStatus.PENDING) { "이미 처리된 receivable입니다. id=$id, status=$status" }
        status = SettlementStatus.COMPLETED
    }

    companion object {
        fun create(
            account: Account,
            orderId: Long,
            settlementDate: LocalDate,
            amount: BigDecimal,
        ): ReceivableSettlement {
            require(orderId > 0) { "orderId는 1 이상이어야 합니다." }
            require(amount > BigDecimal.ZERO) { "정산 금액은 0보다 커야 합니다." }
            return ReceivableSettlement().apply {
                this.account = account
                this.orderId = orderId
                this.settlementDate = settlementDate
                this.amount = amount
                this.status = SettlementStatus.PENDING
            }
        }
    }
}

