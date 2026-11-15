package com.papertrading.api.infrastructure.toss

import com.papertrading.api.application.account.port.BrokerAccountBalancePort
import com.papertrading.api.application.account.port.BrokerAccountRef
import com.papertrading.api.application.account.port.BrokerBalancePosition
import com.papertrading.api.application.account.port.BrokerBalanceSnapshot
import com.papertrading.api.application.account.port.BrokerCurrencyBalance
import com.papertrading.api.common.exception.TossResponseInvalidException
import com.papertrading.api.infrastructure.toss.dto.TossBuyingPower
import com.papertrading.api.infrastructure.toss.dto.TossCurrencyAmounts
import com.papertrading.api.infrastructure.toss.dto.TossHoldingsItem
import com.papertrading.api.infrastructure.toss.dto.TossHoldingsOverview
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDateTime

/**
 * 토스 잔고 어댑터 (D7): holdings 1회(ASSET) + buying-power KRW·USD 2회(ORDER_INFO)를 공통 포트 모양으로 바꿈, 필드표는 step-6 부록 A
 * 통화별 합계는 KRW·USD 두 행 고정이고 환산·합산하지 않음 (FLAG-07), 모르는 통화 종목은 종목에만 싣고 합계에는 넣지 않음
 * 예수금 API 가 없어 cashBalance 는 늘 null, 매수 가능 금액은 buying-power 값 (DEC-04)
 * 수익률은 퍼센트: 종목은 토스 소수 비율에 100 을 곱하고(반올림 없음), 통화별 합계는 KIS 방식으로 직접 계산(scale 6 HALF_UP, ×100, scale 2)
 * 토스 합계 rate 는 원화 환산 전체 기준이라 쓰지 않음, 필수 필드 누락·숫자 변환 실패는 대체값 없이 TOSS_RESPONSE_INVALID (D14)
 */
@Component
class TossAccountBalanceAdapter(
    private val client: TossApiClient,
    private val clock: Clock = Clock.systemDefaultZone(),
) : BrokerAccountBalancePort {

    override fun fetchBalance(account: BrokerAccountRef): BrokerBalanceSnapshot {
        val accountSeq = account.externalAccountId
        val holdings = client.get(holdingsRequest(accountSeq), TossHoldingsOverview::class.java)
        val cashBuyingPowers = BALANCE_CURRENCIES.associateWith { buyingPower(accountSeq, it) }
        return BrokerBalanceSnapshot(
            asOf = LocalDateTime.now(clock),
            currencyBalances = BALANCE_CURRENCIES.map { currencyBalance(it, holdings, cashBuyingPowers.getValue(it)) },
            positions = holdings.items.map(::toPosition),
        )
    }

    private fun holdingsRequest(seq: String) = TossApiRequest(HOLDINGS_PATH, TossRateLimitGroup.ASSET, accountSeq = seq)

    // 응답 통화가 요청 통화와 다르면 다른 통화 금액이 섞이므로 실패
    private fun buyingPower(accountSeq: String, currency: String): BigDecimal {
        val request = TossApiRequest(
            BUYING_POWER_PATH,
            TossRateLimitGroup.ORDER_INFO,
            mapOf(CURRENCY_PARAM to currency),
            accountSeq,
        )
        val power = client.get(request, TossBuyingPower::class.java)
        if (power.currency != currency) throw TossResponseInvalidException("buying-power 응답 통화가 요청 통화와 다름")
        return power.cashBuyingPower
    }

    // usd 가 null 인 통화는 그 칸이 null, 0 으로 채우지 않음
    private fun currencyBalance(
        currency: String,
        holdings: TossHoldingsOverview,
        cashBuyingPower: BigDecimal,
    ): BrokerCurrencyBalance {
        val unrealizedPnl = holdings.profitLoss.amount.of(currency)
        return BrokerCurrencyBalance(
            currency = currency,
            cashBalance = null,
            cashBuyingPower = cashBuyingPower,
            marketValue = holdings.marketValue.amount.of(currency),
            unrealizedPnl = unrealizedPnl,
            returnRate = returnRate(unrealizedPnl, holdings.totalPurchaseAmount.of(currency)),
        )
    }

    // 원금이 0 이거나 값이 없으면 null
    private fun returnRate(unrealizedPnl: BigDecimal?, principal: BigDecimal?): BigDecimal? {
        if (unrealizedPnl == null || principal == null || principal.signum() == 0) return null
        return unrealizedPnl
            .divide(principal, RATE_DIVIDE_SCALE, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(PERCENT_FACTOR))
            .setScale(PERCENT_SCALE, RoundingMode.HALF_UP)
    }

    private fun toPosition(item: TossHoldingsItem) = BrokerBalancePosition(
        ticker = item.symbol,
        currency = item.currency,
        quantity = item.quantity,
        avgPrice = item.averagePurchasePrice,
        currentPrice = item.lastPrice,
        marketValue = item.marketValue.amount,
        unrealizedPnl = item.profitLoss.amount,
        returnRate = item.profitLoss.rate.movePointRight(PERCENT_POINT_SHIFT),
    )

    companion object {
        private const val HOLDINGS_PATH = "/api/v1/holdings"
        private const val BUYING_POWER_PATH = "/api/v1/buying-power"
        private const val CURRENCY_PARAM = "currency"
        private const val RATE_DIVIDE_SCALE = 6
        private const val PERCENT_SCALE = 2
        private const val PERCENT_FACTOR = 100L
        private const val PERCENT_POINT_SHIFT = 2

        // 합계 행은 두 통화로 고정
        private val BALANCE_CURRENCIES = listOf(TossCurrencyAmounts.KRW, TossCurrencyAmounts.USD)
    }
}
