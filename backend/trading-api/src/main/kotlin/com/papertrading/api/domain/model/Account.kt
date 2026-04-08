package com.papertrading.api.domain.model

import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.model.base.BaseAuditEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Check
import java.math.BigDecimal

@Entity
@Table(name = "accounts")
@Check(constraints = "deposit >= 0 AND available_deposit >= 0 AND locked_deposit >= 0")
class Account(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "account_name", nullable = false, length = 100)
    var accountName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    var accountType: AccountType? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "trading_mode", nullable = false, length = 20)
    var tradingMode: TradingMode? = null,

    @Column(name = "deposit", nullable = false, precision = 20, scale = 4)
    var deposit: BigDecimal = BigDecimal.ZERO,

    @Column(name = "available_deposit", nullable = false, precision = 20, scale = 4)
    var availableDeposit: BigDecimal = BigDecimal.ZERO,

    @Column(name = "locked_deposit", nullable = false, precision = 20, scale = 4)
    var lockedDeposit: BigDecimal = BigDecimal.ZERO,

    @Column(name = "base_currency", nullable = false, length = 3)
    var baseCurrency: String = "KRW",

    @Column(name = "external_account_id", length = 100)
    var externalAccountId: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
) : BaseAuditEntity() {

    fun lockDeposit(amount: BigDecimal) {
        availableDeposit = availableDeposit.subtract(amount)
        lockedDeposit = lockedDeposit.add(amount)
    }

    fun unlockDeposit(amount: BigDecimal) {
        lockedDeposit = lockedDeposit.subtract(amount)
        availableDeposit = availableDeposit.add(amount)
    }

    fun confirmBuy(amount: BigDecimal) {
        lockedDeposit = lockedDeposit.subtract(amount)
        deposit = deposit.subtract(amount)
    }

    fun receiveSellProceeds(amount: BigDecimal) {
        deposit = deposit.add(amount)
        availableDeposit = availableDeposit.add(amount)
    }
}
