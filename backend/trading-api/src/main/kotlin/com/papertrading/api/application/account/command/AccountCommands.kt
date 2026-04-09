package com.papertrading.api.application.account.command

import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TradingMode
import java.math.BigDecimal

data class CreateAccountCommand(
    val accountName: String,
    val accountType: AccountType,
    val tradingMode: TradingMode,
    val initialDeposit: BigDecimal,
    val baseCurrency: String = "KRW",
    val externalAccountId: String? = null
)

data class DepositCommand(
    val amount: BigDecimal,
    val idempotencyKey: String,
    val description: String? = null
)

data class WithdrawCommand(
    val amount: BigDecimal,
    val idempotencyKey: String,
    val description: String? = null
)

data class UpdateAccountCommand(
    val accountName: String? = null,
    val externalAccountId: String? = null
)