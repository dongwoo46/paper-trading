package com.papertrading.api.domain.entity.account

import com.papertrading.api.common.exception.BadRequestException
import com.papertrading.api.common.exception.ConflictException
import com.papertrading.api.common.exception.EntityMappingException
import com.papertrading.api.domain.entity.base.BaseAuditEntity
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.enums.TransactionType
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
class Account protected constructor() : BaseAuditEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(name = "account_name", nullable = false, length = 100)
    lateinit var accountName: String
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    lateinit var accountType: AccountType
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "trading_mode", nullable = false, length = 20)
    lateinit var tradingMode: TradingMode
        private set

    @Column(name = "deposit", nullable = false, precision = 20, scale = 4)
    lateinit var deposit: BigDecimal
        private set

    @Column(name = "available_deposit", nullable = false, precision = 20, scale = 4)
    lateinit var availableDeposit: BigDecimal
        private set

    @Column(name = "locked_deposit", nullable = false, precision = 20, scale = 4)
    lateinit var lockedDeposit: BigDecimal
        private set

    @Column(name = "base_currency", nullable = false, length = 3)
    lateinit var baseCurrency: String
        private set

    @Column(name = "external_account_id", length = 100)
    var externalAccountId: String? = null
        private set

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
        private set

    fun addDeposit(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO) { "입금 금액은 0보다 커야 합니다." }
        deposit = deposit.add(amount)
        availableDeposit = availableDeposit.add(amount)
    }

    fun recordDeposit(amount: BigDecimal, idempotencyKey: String, description: String? = null): AccountLedger {
        addDeposit(amount)
        return AccountLedger.create(
            account = this,
            transactionType = TransactionType.DEPOSIT,
            amount = amount,
            balanceAfter = availableDeposit,
            idempotencyKey = idempotencyKey,
            description = description
        )
    }

    fun recordInitialDeposit(idempotencyKey: String): AccountLedger {
        return AccountLedger.create(
            account = this,
            transactionType = TransactionType.DEPOSIT,
            amount = deposit,
            balanceAfter = availableDeposit,
            idempotencyKey = idempotencyKey
        )
    }

    fun withdraw(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO) { "출금 금액은 0보다 커야 합니다." }
        require(availableDeposit >= amount) { "가용 예수금이 부족합니다." }
        deposit = deposit.subtract(amount)
        availableDeposit = availableDeposit.subtract(amount)
    }

    fun recordWithdrawal(amount: BigDecimal, idempotencyKey: String, description: String? = null): AccountLedger {
        withdraw(amount)
        return AccountLedger.create(
            account = this,
            transactionType = TransactionType.WITHDRAWAL,
            amount = amount,
            balanceAfter = availableDeposit,
            idempotencyKey = idempotencyKey,
            description = description
        )
    }

    fun lockDeposit(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO) { "잠금 금액은 0보다 커야 합니다." }
        require(availableDeposit >= amount) { "가용 예수금이 부족합니다." }
        availableDeposit = availableDeposit.subtract(amount)
        lockedDeposit = lockedDeposit.add(amount)
    }

    fun recordBuyLock(amount: BigDecimal, refOrderId: Long, idempotencyKey: String): AccountLedger {
        lockDeposit(amount)
        return AccountLedger.create(
            account = this,
            transactionType = TransactionType.BUY_LOCK,
            amount = amount,
            balanceAfter = availableDeposit,
            refOrderId = refOrderId,
            idempotencyKey = idempotencyKey
        )
    }

    fun unlockDeposit(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO) { "해제 금액은 0보다 커야 합니다." }
        require(lockedDeposit >= amount) { "잠금 예수금이 부족합니다." }
        lockedDeposit = lockedDeposit.subtract(amount)
        availableDeposit = availableDeposit.add(amount)
    }

    fun recordBuyUnlock(amount: BigDecimal, refOrderId: Long, idempotencyKey: String): AccountLedger {
        unlockDeposit(amount)
        return AccountLedger.create(
            account = this,
            transactionType = TransactionType.BUY_UNLOCK,
            amount = amount,
            balanceAfter = availableDeposit,
            refOrderId = refOrderId,
            idempotencyKey = idempotencyKey
        )
    }

    fun confirmBuy(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO) { "매수 확정 금액은 0보다 커야 합니다." }
        require(lockedDeposit >= amount) { "잠금 예수금이 부족합니다." }
        lockedDeposit = lockedDeposit.subtract(amount)
        deposit = deposit.subtract(amount)
    }

    fun recordBuyExecution(cost: BigDecimal, refOrderId: Long, refExecutionId: Long, idempotencyKey: String): AccountLedger {
        confirmBuy(cost)
        return AccountLedger.create(
            account = this,
            transactionType = TransactionType.BUY_EXECUTE,
            amount = cost,
            balanceAfter = availableDeposit,
            refOrderId = refOrderId,
            refExecutionId = refExecutionId,
            idempotencyKey = idempotencyKey
        )
    }

    fun receiveSellProceeds(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO) { "매도 대금은 0보다 커야 합니다." }
        deposit = deposit.add(amount)
        availableDeposit = availableDeposit.add(amount)
    }

    fun recordSellExecution(netProceeds: BigDecimal, refOrderId: Long, refExecutionId: Long, idempotencyKey: String): AccountLedger {
        return AccountLedger.create(
            account = this,
            transactionType = TransactionType.SELL_EXECUTE,
            amount = netProceeds,
            balanceAfter = availableDeposit,
            refOrderId = refOrderId,
            refExecutionId = refExecutionId,
            idempotencyKey = idempotencyKey
        )
    }

    fun recordFee(fee: BigDecimal, refOrderId: Long, refExecutionId: Long, idempotencyKey: String): AccountLedger {
        return AccountLedger.create(
            account = this,
            transactionType = TransactionType.FEE,
            amount = fee,
            balanceAfter = availableDeposit,
            refOrderId = refOrderId,
            refExecutionId = refExecutionId,
            idempotencyKey = idempotencyKey
        )
    }

    fun recordDepositWithLedger(amount: BigDecimal, idempotencyKey: String): AccountLedger {
        receiveSellProceeds(amount)
        return AccountLedger.create(
            account = this,
            transactionType = TransactionType.SETTLEMENT,
            amount = amount,
            balanceAfter = availableDeposit,
            idempotencyKey = idempotencyKey
        )
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

    fun activate() {
        check(!isActive) { "이미 활성화된 계좌입니다." }
        isActive = true
    }

    fun updateRiskPolicy(
        existing: RiskPolicy,
        maxPositionRatio: BigDecimal?,
        maxDailyLoss: BigDecimal?,
        maxOrderAmount: BigDecimal?
    ): RiskPolicy {
        existing.deactivate()
        return createRiskPolicy(maxPositionRatio, maxDailyLoss, maxOrderAmount)
    }

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
        return RiskPolicy.create(
            account = this,
            maxPositionRatio = maxPositionRatio,
            maxDailyLoss = maxDailyLoss,
            maxOrderAmount = maxOrderAmount
        )
    }

    fun createExitTriggerDefault(
        enabled: Boolean,
        stopLossPercent: BigDecimal?,
        takeProfitPercent: BigDecimal?,
    ): AccountExitTriggerDefault {
        validateExitTriggerDefault(enabled, stopLossPercent, takeProfitPercent)
        val accountId = id ?: throw EntityMappingException("account id가 없습니다.")
        return AccountExitTriggerDefault.create(
            accountId = accountId,
            enabled = enabled,
            stopLossPercent = stopLossPercent,
            takeProfitPercent = takeProfitPercent,
        )
    }

    fun updateExitTriggerDefault(
        existing: AccountExitTriggerDefault,
        enabled: Boolean,
        stopLossPercent: BigDecimal?,
        takeProfitPercent: BigDecimal?,
    ): AccountExitTriggerDefault {
        val accountId = id ?: throw EntityMappingException("account id가 없습니다.")
        if (existing.accountId != accountId) {
            throw ConflictException(
                "ACCOUNT_EXIT_TRIGGER_OWNER_MISMATCH",
                "계좌 소유자 불일치: accountId=$accountId, defaultAccountId=${existing.accountId}"
            )
        }
        validateExitTriggerDefault(enabled, stopLossPercent, takeProfitPercent)
        existing.enabled = enabled
        existing.stopLossPercent = stopLossPercent
        existing.takeProfitPercent = takeProfitPercent
        return existing
    }

    private fun validateExitTriggerDefault(
        enabled: Boolean,
        stopLossPercent: BigDecimal?,
        takeProfitPercent: BigDecimal?,
    ) {
        validatePercentScale(stopLossPercent)
        validatePercentScale(takeProfitPercent)
        validatePercentRange(stopLossPercent)
        validatePercentRange(takeProfitPercent)
        if (enabled && stopLossPercent == null && takeProfitPercent == null) {
            throw BadRequestException(
                "EXIT_TRIGGER_PERCENT_REQUIRED",
                "enabled trigger needs at least one percent"
            )
        }
    }

    private fun validatePercentScale(value: BigDecimal?) {
        if (value != null && value.stripTrailingZeros().scale() > 4) {
            throw BadRequestException("INVALID_PERCENT_SCALE", "percent scale must be <= 4")
        }
    }

    private fun validatePercentRange(value: BigDecimal?) {
        if (value != null && (value <= BigDecimal.ZERO || value >= BigDecimal("100"))) {
            throw BadRequestException("INVALID_PERCENT_RANGE", "percent must be in (0,100)")
        }
    }

    companion object {
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

            return Account().apply {
                this.accountName = accountName
                this.accountType = accountType
                this.tradingMode = tradingMode
                this.deposit = initialDeposit
                this.availableDeposit = initialDeposit
                this.lockedDeposit = BigDecimal.ZERO
                this.baseCurrency = baseCurrency.uppercase()
                this.externalAccountId = externalAccountId
                this.isActive = true
            }
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
