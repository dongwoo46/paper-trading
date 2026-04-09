package com.papertrading.api.presentation.dto.account

import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TradingMode
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateAccountRequest(
    @field:NotBlank val accountName: String,
    @field:NotNull val accountType: AccountType,
    @field:NotNull val tradingMode: TradingMode,
    @field:NotNull @field:DecimalMin("0") val initialDeposit: BigDecimal,
    @field:Size(min = 3, max = 3) val baseCurrency: String = "KRW",
    val externalAccountId: String? = null
)

data class UpdateAccountRequest(
    val accountName: String? = null,
    val externalAccountId: String? = null
)

data class DepositRequest(
    @field:NotNull @field:DecimalMin("0.0001") val amount: BigDecimal,
    @field:NotBlank val idempotencyKey: String,
    val description: String? = null
)

data class WithdrawRequest(
    @field:NotNull @field:DecimalMin("0.0001") val amount: BigDecimal,
    @field:NotBlank val idempotencyKey: String,
    val description: String? = null
)