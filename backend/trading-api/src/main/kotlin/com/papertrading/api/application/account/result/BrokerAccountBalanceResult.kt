package com.papertrading.api.application.account.result

import com.papertrading.api.application.account.port.BrokerBalancePosition
import com.papertrading.api.application.account.port.BrokerCurrencyBalance
import com.papertrading.api.domain.enums.AccountSource
import com.papertrading.api.domain.enums.TradingMode
import java.time.LocalDateTime

/** 브로커 공통 잔고 조회 결과, 값 타입은 포트의 것을 그대로 쓰고 계좌 식별·출처만 더함 */
data class BrokerAccountBalanceResult(
    val accountId: Long,
    val source: AccountSource,
    val tradingMode: TradingMode,
    val asOf: LocalDateTime,
    val currencyBalances: List<BrokerCurrencyBalance>,
    val positions: List<BrokerBalancePosition>,
)
