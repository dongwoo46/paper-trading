package com.papertrading.api.presentation.dto.account

import com.papertrading.api.application.account.port.BrokerBalancePosition
import com.papertrading.api.application.account.port.BrokerCurrencyBalance
import com.papertrading.api.application.account.result.BrokerAccountBalanceResult
import com.papertrading.api.domain.enums.AccountSource
import com.papertrading.api.domain.enums.TradingMode
import java.math.BigDecimal
import java.time.LocalDateTime

/** 브로커 공통 잔고 응답, 금액·수량·가격·비율은 JSON 문자열(toPlainString, FLAG-10) — 새 API 에만 적용하고 기존 API 응답은 그대로 */
data class BrokerAccountBalanceResponse(
    val accountId: Long,
    val source: AccountSource,
    val tradingMode: TradingMode,
    val asOf: LocalDateTime,
    val currencyBalances: List<BrokerCurrencyBalanceResponse>,
    val positions: List<BrokerBalancePositionResponse>,
) {
    companion object {
        fun from(result: BrokerAccountBalanceResult) = BrokerAccountBalanceResponse(
            accountId = result.accountId,
            source = result.source,
            tradingMode = result.tradingMode,
            asOf = result.asOf,
            currencyBalances = result.currencyBalances.map(BrokerCurrencyBalanceResponse::from),
            positions = result.positions.map(BrokerBalancePositionResponse::from),
        )
    }
}

data class BrokerCurrencyBalanceResponse(
    val currency: String,
    val cashBalance: String?,
    val cashBuyingPower: String?,
    val marketValue: String?,
    val unrealizedPnl: String?,
    val returnRate: String?,
) {
    companion object {
        fun from(balance: BrokerCurrencyBalance) = BrokerCurrencyBalanceResponse(
            currency = balance.currency,
            cashBalance = balance.cashBalance.plain(),
            cashBuyingPower = balance.cashBuyingPower.plain(),
            marketValue = balance.marketValue.plain(),
            unrealizedPnl = balance.unrealizedPnl.plain(),
            returnRate = balance.returnRate.plain(),
        )
    }
}

data class BrokerBalancePositionResponse(
    val ticker: String,
    val currency: String,
    val quantity: String,
    val avgPrice: String,
    val currentPrice: String,
    val marketValue: String,
    val unrealizedPnl: String,
    val returnRate: String,
) {
    companion object {
        fun from(position: BrokerBalancePosition) = BrokerBalancePositionResponse(
            ticker = position.ticker,
            currency = position.currency,
            quantity = position.quantity.toPlainString(),
            avgPrice = position.avgPrice.toPlainString(),
            currentPrice = position.currentPrice.toPlainString(),
            marketValue = position.marketValue.toPlainString(),
            unrealizedPnl = position.unrealizedPnl.toPlainString(),
            returnRate = position.returnRate.toPlainString(),
        )
    }
}

// 지수 표기(1E+3) 없이 자릿수 그대로
private fun BigDecimal?.plain(): String? = this?.toPlainString()
