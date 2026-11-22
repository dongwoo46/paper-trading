package com.papertrading.api.application.account.port

import com.papertrading.api.common.maskAllButLastFour
import com.papertrading.api.domain.enums.TradingMode
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 브로커 공통 잔고 조회 포트 (D7), 구현은 토스와 KIS
 * 브로커가 주지 않는 값은 0 이 아니라 null, supportedModes 는 서비스가 모드 → 구현 표를 만드는 근거 (D25)
 */
interface BrokerAccountBalancePort {
    val supportedModes: Set<TradingMode>

    fun fetchBalance(account: BrokerAccountRef): BrokerBalanceSnapshot
}

/** 포트에 넘기는 계좌 식별값, externalAccountId 는 브로커 쪽 계좌 키(토스는 accountSeq)이고 문자열 표현은 maskAllButLastFour 로만 보임 */
data class BrokerAccountRef(
    val accountId: Long,
    val tradingMode: TradingMode,
    val externalAccountId: String,
) {
    private val maskedExternalAccountId: String get() = maskAllButLastFour(externalAccountId)

    override fun toString(): String = "BrokerAccountRef(accountId=$accountId, seq=$maskedExternalAccountId)"
}

/** 조회 시각(시간대 없는 KST)과 통화별 합계, 종목별 보유 */
data class BrokerBalanceSnapshot(
    val asOf: LocalDateTime,
    val currencyBalances: List<BrokerCurrencyBalance>,
    val positions: List<BrokerBalancePosition>,
)

/** 통화별 합계, 통화끼리 환산·합산하지 않음 (FLAG-07), 수익률은 퍼센트 */
data class BrokerCurrencyBalance(
    val currency: String,
    val cashBalance: BigDecimal?,
    val cashBuyingPower: BigDecimal?,
    val marketValue: BigDecimal?,
    val unrealizedPnl: BigDecimal?,
    val returnRate: BigDecimal?,
)

/** 종목별 보유, 수익률은 퍼센트, 모르는 통화 문자열도 그대로 */
data class BrokerBalancePosition(
    val ticker: String,
    val currency: String,
    val quantity: BigDecimal,
    val avgPrice: BigDecimal,
    val currentPrice: BigDecimal,
    val marketValue: BigDecimal,
    val unrealizedPnl: BigDecimal,
    val returnRate: BigDecimal,
)
