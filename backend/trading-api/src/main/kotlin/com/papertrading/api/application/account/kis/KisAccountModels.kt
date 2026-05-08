package com.papertrading.api.application.account.kis

import com.papertrading.api.common.exception.ApiDomainException
import com.papertrading.api.domain.enums.AccountSource
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.OffsetDateTime

enum class KisAccountMode { LIVE, PAPER }

data class KisBalancePosition(
    val ticker: String,
    val quantity: BigDecimal,
    val avgPrice: BigDecimal,
    val currentPrice: BigDecimal,
    val marketValue: BigDecimal,
    val unrealizedPnl: BigDecimal,
    val returnRate: BigDecimal
)

data class KisBalanceSnapshot(
    val asOf: OffsetDateTime,
    val cashBalance: BigDecimal,
    val marketValue: BigDecimal,
    val unrealizedPnl: BigDecimal,
    val returnRate: BigDecimal,
    val positions: List<KisBalancePosition>
)

interface KisAccountBalancePort {
    fun fetchBalance(accountId: Long, trId: String): KisBalanceSnapshot
}

data class KisBalancePositionResult(
    val ticker: String,
    val quantity: BigDecimal,
    val avgPrice: BigDecimal,
    val currentPrice: BigDecimal,
    val marketValue: BigDecimal,
    val unrealizedPnl: BigDecimal,
    val returnRate: BigDecimal
)

data class KisReconciliationResult(
    val missingInLocal: List<String>,
    val missingInKis: List<String>,
    val quantityMismatch: List<String>
)

data class KisAccountBalanceResult(
    val accountId: Long,
    val source: AccountSource = AccountSource.KIS,
    val mode: KisAccountMode,
    val asOf: OffsetDateTime,
    val cashBalance: BigDecimal,
    val marketValue: BigDecimal,
    val unrealizedPnl: BigDecimal,
    val returnRate: BigDecimal,
    val positions: List<KisBalancePositionResult>,
    val reconciliation: KisReconciliationResult
)

class KisAuthorizationException(message: String, cause: Throwable? = null) :
    ApiDomainException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, message, cause)
class KisForbiddenException(message: String, cause: Throwable? = null) :
    ApiDomainException("FORBIDDEN", HttpStatus.FORBIDDEN, message, cause)
class KisRemoteCallException(message: String, cause: Throwable? = null) :
    ApiDomainException("BAD_GATEWAY", HttpStatus.BAD_GATEWAY, message, cause)
class KisTimeoutException(message: String, cause: Throwable? = null) :
    ApiDomainException("GATEWAY_TIMEOUT", HttpStatus.GATEWAY_TIMEOUT, message, cause)
