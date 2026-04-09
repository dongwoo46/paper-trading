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

/**
 * 계좌 (Aggregate Root)
 * deposit = 총 예수금, availableDeposit = 주문 가능 금액, lockedDeposit = 주문 잠금 금액
 * 입금/출금/잠금/매수·매도 확정 등 자금 흐름의 핵심 도메인 객체
 */
@Entity
@Table(name = "accounts")
@Check(constraints = "deposit >= 0 AND available_deposit >= 0 AND locked_deposit >= 0")
class Account protected constructor(
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

    // 입금: 총 예수금과 가용 예수금을 동시에 증가시킨다.
    fun deposit(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO) { "입금 금액은 0보다 커야 합니다." }
        deposit = deposit.add(amount)
        availableDeposit = availableDeposit.add(amount)
    }

    // 출금: 가용 예수금이 충분한 경우에만 감소시킨다.
    fun withdraw(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO) { "출금 금액은 0보다 커야 합니다." }
        require(availableDeposit >= amount) { "가용 예수금이 부족합니다." }
        deposit = deposit.subtract(amount)
        availableDeposit = availableDeposit.subtract(amount)
    }

    fun lockDeposit(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO) { "잠금 금액은 0보다 커야 합니다." }
        require(availableDeposit >= amount) { "가용 예수금이 부족합니다." }
        availableDeposit = availableDeposit.subtract(amount)
        lockedDeposit = lockedDeposit.add(amount)
    }

    fun unlockDeposit(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO) { "해제 금액은 0보다 커야 합니다." }
        require(lockedDeposit >= amount) { "잠금 예수금이 부족합니다." }
        lockedDeposit = lockedDeposit.subtract(amount)
        availableDeposit = availableDeposit.add(amount)
    }

    fun confirmBuy(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO) { "매수 확정 금액은 0보다 커야 합니다." }
        require(lockedDeposit >= amount) { "잠금 예수금이 부족합니다." }
        lockedDeposit = lockedDeposit.subtract(amount)
        deposit = deposit.subtract(amount)
    }

    fun receiveSellProceeds(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO) { "매도 대금은 0보다 커야 합니다." }
        deposit = deposit.add(amount)
        availableDeposit = availableDeposit.add(amount)
    }

    fun rename(newName: String) {
        require(newName.isNotBlank()) { "계좌명은 비어 있을 수 없습니다." }
        accountName = newName
    }

    fun updateExternalAccountId(externalId: String?) {
        externalAccountId = externalId
    }

    fun deactivate() {
        check(isActive) { "이미 비활성화된 계좌입니다." }
        isActive = false
    }

    // RiskPolicy 생성은 반드시 Account(Aggregate Root)를 통해 수행한다
    fun createRiskPolicy(
        maxPositionRatio: BigDecimal?,
        maxDailyLoss: BigDecimal?,
        maxOrderAmount: BigDecimal?
    ): RiskPolicy {
        require(maxPositionRatio == null || maxPositionRatio in BigDecimal.ZERO..BigDecimal.ONE) {
            "maxPositionRatio는 0~1 사이여야 합니다."
        }
        require(maxDailyLoss == null || maxDailyLoss > BigDecimal.ZERO) {
            "maxDailyLoss는 0보다 커야 합니다."
        }
        require(maxOrderAmount == null || maxOrderAmount > BigDecimal.ZERO) {
            "maxOrderAmount는 0보다 커야 합니다."
        }
        return RiskPolicy(
            account = this,
            maxPositionRatio = maxPositionRatio,
            maxDailyLoss = maxDailyLoss,
            maxOrderAmount = maxOrderAmount,
            isActive = true
        )
    }

    companion object {
        // 계좌 생성 규칙을 도메인 내부에 고정한다.
        fun create(
            accountName: String,
            accountType: AccountType,
            tradingMode: TradingMode,
            initialDeposit: BigDecimal,
            baseCurrency: String = "KRW",
            externalAccountId: String? = null
        ): Account {
            require(accountName.isNotBlank()) { "계좌명은 비어 있을 수 없습니다." }
            require(initialDeposit >= BigDecimal.ZERO) { "초기 예수금은 0 이상이어야 합니다." }
            require(baseCurrency.length == 3) { "기준 통화는 3자리 코드여야 합니다." }
            validateTradingMode(accountType, tradingMode)

            return Account(
                accountName = accountName,
                accountType = accountType,
                tradingMode = tradingMode,
                deposit = initialDeposit,
                availableDeposit = initialDeposit,
                lockedDeposit = BigDecimal.ZERO,
                baseCurrency = baseCurrency.uppercase(),
                externalAccountId = externalAccountId,
                isActive = true
            )
        }

        private fun validateTradingMode(accountType: AccountType, tradingMode: TradingMode) {
            val isValid = when (accountType) {
                AccountType.STOCK -> tradingMode == TradingMode.LOCAL ||
                    tradingMode == TradingMode.KIS_PAPER ||
                    tradingMode == TradingMode.KIS_LIVE
                AccountType.CRYPTO -> tradingMode == TradingMode.LOCAL || tradingMode == TradingMode.UPBIT_LIVE
            }
            require(isValid) { "계좌 타입과 거래 모드 조합이 올바르지 않습니다." }
        }
    }
}
